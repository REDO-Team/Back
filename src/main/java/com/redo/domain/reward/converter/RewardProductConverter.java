package com.redo.domain.reward.converter;

import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPageResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductResponseDTO;
import com.redo.domain.reward.entity.RewardProduct;
import org.springframework.data.domain.Page;

public class RewardProductConverter {

    private RewardProductConverter() {
    }

    public static RewardProductResponseDTO toRewardProductResponse(
            RewardProduct rewardProduct,
            String imageUrl
    ) {
        return new RewardProductResponseDTO(
                rewardProduct.getId(),
                rewardProduct.getRewardProductType(),
                rewardProduct.getName(),
                imageUrl,
                rewardProduct.getPricePoint(),
                rewardProduct.getStockQuantity(),
                rewardProduct.getStatus()
        );
    }

    public static RewardProductDetailResponseDTO toRewardProductDetailResponse(
            RewardProduct rewardProduct,
            String imageUrl
    ) {
        return new RewardProductDetailResponseDTO(
                rewardProduct.getId(),
                rewardProduct.getRewardProductType(),
                rewardProduct.getName(),
                rewardProduct.getDescription(),
                rewardProduct.getUsageGuide(),
                rewardProduct.getValidityDays(),
                imageUrl,
                rewardProduct.getPricePoint(),
                rewardProduct.getStockQuantity(),
                rewardProduct.getStatus()
        );
    }

    public static RewardProductPageResponseDTO toRewardProductPageResponse(Page<RewardProductResponseDTO> page) {
        return new RewardProductPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}
