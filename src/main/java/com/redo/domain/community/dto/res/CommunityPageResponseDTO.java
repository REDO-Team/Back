package com.redo.domain.community.dto.res;

import java.util.List;

public record CommunityPageResponseDTO(
        List<CommunityResponseDTO> items,
        int page,
        int size,
        boolean hasNext
) {
}
