package com.redo.domain.reward.dto.res;

import com.redo.domain.reward.enums.RewardFulfillmentStatus;
import com.redo.domain.reward.enums.RewardFulfillmentType;
import com.redo.domain.reward.enums.RewardRedemptionStatus;

import java.time.LocalDateTime;

public record RewardRedemptionHistoryResponseDTO(
        Long rewardRedemptionId,
        Long rewardProductId,
        String productName,
        String productImageUrl,
        Integer usedPoint,
        RewardRedemptionStatus redemptionStatus,
        RewardFulfillmentType fulfillmentType,
        RewardFulfillmentStatus fulfillmentStatus,
        LocalDateTime redeemedAt
) {
}
