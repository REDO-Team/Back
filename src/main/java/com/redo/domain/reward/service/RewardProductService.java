package com.redo.domain.reward.service;

import com.redo.domain.reward.converter.RewardProductConverter;
import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductResponseDTO;
import com.redo.domain.reward.entity.RewardProduct;
import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.repository.RewardProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardProductService {

    private static final int MIN_STOCK_QUANTITY = 0;

    private final RewardProductRepository rewardProductRepository;

    // 상품 목록 조회 로직
    public Page<RewardProductResponseDTO> getRewardProducts(
            RewardProductType rewardProductType,
            Pageable pageable
    ) {
        Page<RewardProduct> rewardProducts = rewardProductType == null
                ? rewardProductRepository.findByStatusAndStockQuantityGreaterThan(
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY,
                        pageable
                )
                : rewardProductRepository.findByRewardProductTypeAndStatusAndStockQuantityGreaterThan(
                        rewardProductType,
                        RewardProductStatus.ACTIVE,
                        MIN_STOCK_QUANTITY,
                        pageable
                );

        return rewardProducts.map(RewardProductConverter::toRewardProductResponse);
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

        return RewardProductConverter.toRewardProductDetailResponse(rewardProduct);
    }
}
