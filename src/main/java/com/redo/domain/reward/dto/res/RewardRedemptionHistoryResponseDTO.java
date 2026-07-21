package com.redo.domain.reward.dto.res;

import com.redo.domain.reward.enums.RewardFulfillmentStatus;
import com.redo.domain.reward.enums.RewardFulfillmentType;

import java.time.LocalDateTime;

public record RewardRedemptionHistoryResponseDTO(
        Long rewardRedemptionId,
        Long rewardProductId,
        String productName,
        String productImageUrl,
        Integer usedPoint,
        RewardFulfillmentType fulfillmentType,
        RewardFulfillmentStatus fulfillmentStatus,
        LocalDateTime redeemedAt
) {
}
