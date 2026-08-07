package com.redo.domain.reward.service;

import com.redo.domain.reward.converter.RewardProductConverter;
import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPageResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPreviewResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductResponseDTO;
import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.repository.RewardProductRepository;
import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardProductService {

    private static final int MIN_STOCK_QUANTITY = 0;
    private static final int PREVIEW_PRODUCT_COUNT = 2;
    private static final ZoneId PREVIEW_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final RewardProductRepository rewardProductRepository;
    private final RewardProductPreviewCacheService rewardProductPreviewCacheService;
    private final S3Service s3Service;

    // 상품 목록 조회 로직
    public RewardProductPageResponseDTO getRewardProducts(
            RewardProductType rewardProductType,
            Long cursor,
            int size
    ) {
        List<RewardProduct> rewardProducts = rewardProductRepository.findAvailableProducts(
                rewardProductType,
                RewardProductStatus.ACTIVE,
                MIN_STOCK_QUANTITY,
                cursor,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = rewardProducts.size() > size;
        List<RewardProduct> pageProducts = hasNext
                ? rewardProducts.subList(0, size)
                : rewardProducts;
        List<RewardProductResponseDTO> items = pageProducts.stream()
                .map(rewardProduct -> RewardProductConverter.toRewardProductResponse(
                        rewardProduct,
                        createImageUrl(rewardProduct.getImageKey())
                ))
                .toList();
        Long nextCursor = hasNext && !pageProducts.isEmpty()
                ? pageProducts.get(pageProducts.size() - 1).getId()
                : null;

        return RewardProductConverter.toRewardProductPageResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    // 상품 상세 조회 로직
    public RewardProductDetailResponseDTO getRewardProduct(Long rewardProductId) {
        RewardProduct rewardProduct = rewardProductRepository
                .findByIdAndStatusAndStockQuantityGreaterThan(
                        rewardProductId,
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY
                )
                .orElseThrow(() -> new RewardException(RewardErrorCode.REWARD_PRODUCT_NOT_FOUND));

        return RewardProductConverter.toRewardProductDetailResponse(
                rewardProduct,
                createImageUrl(rewardProduct.getImageKey())
        );
    }

    // 홈 화면에 노출할 상품을 사용자별로 하루 동안 동일하게 유지하는 미리보기 조회 로직
    public RewardProductPreviewResponseDTO getRewardProductPreview(Long userId) {
        ZonedDateTime now = ZonedDateTime.now(PREVIEW_ZONE_ID);
        LocalDate previewDate = now.toLocalDate();
        Optional<List<Long>> cachedProductIds = rewardProductPreviewCacheService.getProductIds(
                userId,
                previewDate
        );
        List<RewardProduct> selectedProducts = cachedProductIds
                .map(this::getAvailableProductsInCachedOrder)
                .orElseGet(List::of);

        if (selectedProducts.size() < PREVIEW_PRODUCT_COUNT) {
            selectedProducts = fillPreviewProducts(
                    selectedProducts,
                    createDailySeed(userId, previewDate)
            );
        }

        List<Long> selectedProductIds = selectedProducts.stream()
                .map(RewardProduct::getId)
                .toList();
        if (!selectedProductIds.isEmpty()
                && cachedProductIds.map(ids -> !ids.equals(selectedProductIds)).orElse(true)) {
            rewardProductPreviewCacheService.putProductIds(
                    userId,
                    previewDate,
                    selectedProductIds,
                    calculateCacheTtl(now)
            );
        }

        List<RewardProductResponseDTO> items = selectedProducts.stream()
                .map(rewardProduct -> RewardProductConverter.toRewardProductResponse(
                        rewardProduct,
                        createImageUrl(rewardProduct.getImageKey())
                ))
                .toList();
        return RewardProductConverter.toRewardProductPreviewResponse(items);
    }

    private List<RewardProduct> getAvailableProductsInCachedOrder(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }

        Map<Long, RewardProduct> availableProductsById = rewardProductRepository
                .findAvailableProductsByIdIn(
                        productIds,
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY
                ).stream()
                .collect(Collectors.toMap(RewardProduct::getId, Function.identity()));

        return productIds.stream()
                .distinct()
                .map(availableProductsById::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<RewardProduct> fillPreviewProducts(
            List<RewardProduct> selectedProducts,
            long seed
    ) {
        int requiredCount = PREVIEW_PRODUCT_COUNT - selectedProducts.size();
        List<Long> selectedProductIds = selectedProducts.stream()
                .map(RewardProduct::getId)
                .toList();
        List<RewardProduct> additionalProducts = selectedProductIds.isEmpty()
                ? rewardProductRepository.findRandomAvailableProducts(
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY,
                        seed,
                        PageRequest.of(0, requiredCount)
                )
                : rewardProductRepository.findRandomAvailableProductsExcludingIds(
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY,
                        selectedProductIds,
                        seed,
                        PageRequest.of(0, requiredCount)
                );

        return Stream.concat(
                        selectedProducts.stream(),
                        additionalProducts.stream()
                )
                .limit(PREVIEW_PRODUCT_COUNT)
                .toList();
    }

    private long createDailySeed(Long userId, LocalDate previewDate) {
        return userId * 31L + previewDate.toEpochDay();
    }

    private Duration calculateCacheTtl(ZonedDateTime now) {
        ZonedDateTime nextMidnight = now.toLocalDate()
                .plusDays(1)
                .atStartOfDay(PREVIEW_ZONE_ID);
        return Duration.between(now, nextMidnight);
    }

    // S3 객체 키를 상품 이미지 조회용 Presigned URL로 변환하는 로직
    private String createImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        return s3Service.createPresignedUrl(imageKey);
    }
}
