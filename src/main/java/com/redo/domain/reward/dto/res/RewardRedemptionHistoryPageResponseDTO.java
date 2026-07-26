package com.redo.domain.reward.dto.res;

import java.util.List;

public record RewardRedemptionHistoryPageResponseDTO(
        List<RewardRedemptionHistoryResponseDTO> content,
        Long nextCursor,
        boolean hasNext
) {
}
