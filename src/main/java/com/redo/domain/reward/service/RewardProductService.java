package com.redo.domain.reward.service;

import com.redo.domain.reward.converter.RewardProductConverter;
import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPageResponseDTO;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardProductService {

    private static final int MIN_STOCK_QUANTITY = 0;

    private final RewardProductRepository rewardProductRepository;
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

    // S3 객체 키를 상품 이미지 조회용 Presigned URL로 변환하는 로직
    private String createImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        return s3Service.createPresignedUrl(imageKey);
    }
}
