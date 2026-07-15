package com.redo.domain.reward.dto.res;

import com.redo.domain.reward.enums.RewardProductStatus;
import com.redo.domain.reward.enums.RewardProductType;

public record RewardProductResponseDTO(
        Long rewardProductId,
        RewardProductType rewardProductType,
        String name,
        String imageUrl,
        Integer pricePoint,
        Integer stockQuantity,
        RewardProductStatus status
) {
}
