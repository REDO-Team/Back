package com.redo.domain.reward.dto.res;

import java.util.List;

public record RewardRedemptionHistoryPageResponseDTO(
        List<RewardRedemptionHistoryResponseDTO> content,
        int page,
        int size,
        boolean hasNext
) {
}
