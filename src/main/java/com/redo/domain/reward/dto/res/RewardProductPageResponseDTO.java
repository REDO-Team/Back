package com.redo.domain.reward.dto.res;

import java.util.List;

public record RewardProductPageResponseDTO(
        List<RewardProductResponseDTO> items,
        Long nextCursor,
        boolean hasNext
) {
}
