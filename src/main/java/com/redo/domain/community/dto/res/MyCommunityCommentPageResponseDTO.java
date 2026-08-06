package com.redo.domain.community.dto.res;

import java.util.List;

public record MyCommunityCommentPageResponseDTO(
        List<MyCommunityCommentResponseDTO> items,
        int page,
        int size,
        boolean hasNext
) {
}
