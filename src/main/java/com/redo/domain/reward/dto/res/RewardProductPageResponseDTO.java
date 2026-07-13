package com.redo.domain.reward.dto.res;

import java.util.List;

public record RewardProductPageResponseDTO(
        List<RewardProductResponseDTO> items,
        int page,
        int size,
        boolean hasNext
) {
}
