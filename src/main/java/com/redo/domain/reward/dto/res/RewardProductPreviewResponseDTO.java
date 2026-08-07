package com.redo.domain.reward.dto.res;

import java.util.List;

public record RewardProductPreviewResponseDTO(
        List<RewardProductResponseDTO> items
) {
}
