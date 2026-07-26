package com.redo.domain.reward.converter;

import com.redo.domain.reward.dto.res.RewardRedemptionHistoryPageResponseDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionHistoryResponseDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionResponseDTO;
import com.redo.domain.reward.entity.RewardFulfillment;
import com.redo.domain.reward.entity.RewardRedemption;

import java.util.List;

public class RewardRedemptionConverter {

    private RewardRedemptionConverter() {
    }

    public static RewardRedemptionResponseDTO toRewardRedemptionResponse(
            RewardRedemption redemption,
            Integer remainingPoint
    ) {
        return new RewardRedemptionResponseDTO(
                redemption.getId(),
                redemption.getRewardProduct().getId(),
                redemption.getProductName(),
                redemption.getUsedPoint(),
                remainingPoint
        );
    }

    public static RewardRedemptionHistoryResponseDTO toRewardRedemptionHistoryResponse(
            RewardFulfillment fulfillment,
            String productImageUrl
    ) {
        RewardRedemption redemption = fulfillment.getRewardRedemption();

        return new RewardRedemptionHistoryResponseDTO(
                redemption.getId(),
                redemption.getRewardProduct().getId(),
                redemption.getProductName(),
                productImageUrl,
                redemption.getUsedPoint(),
                fulfillment.getRewardFulfillmentType(),
                fulfillment.getStatus(),
                redemption.getCreatedAt()
        );
    }

    public static RewardRedemptionHistoryPageResponseDTO toRewardRedemptionHistoryPageResponse(
            List<RewardRedemptionHistoryResponseDTO> content,
            Long nextCursor,
            boolean hasNext
    ) {
        return new RewardRedemptionHistoryPageResponseDTO(
                content,
                nextCursor,
                hasNext
        );
    }
}
