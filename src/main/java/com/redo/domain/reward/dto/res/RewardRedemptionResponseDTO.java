package com.redo.domain.reward.dto.res;

import com.redo.domain.reward.enums.RewardRedemptionStatus;

public record RewardRedemptionResponseDTO(
        Long rewardRedemptionId,
        Long rewardProductId,
        String productName,
        Integer usedPoint,
        Integer remainingPoint,
        RewardRedemptionStatus status
) {
}
