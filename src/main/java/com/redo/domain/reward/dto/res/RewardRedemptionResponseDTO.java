package com.redo.domain.reward.dto.res;

public record RewardRedemptionResponseDTO(
        Long rewardRedemptionId,
        Long rewardProductId,
        String productName,
        Integer usedPoint,
        Integer remainingPoint
) {
}
