package com.redo.domain.community.dto.res;

import java.util.List;

public record MyCommunityPageResponseDTO(
        List<MyCommunityResponseDTO> items,
        int page,
        int size,
        boolean hasNext
) {
}
