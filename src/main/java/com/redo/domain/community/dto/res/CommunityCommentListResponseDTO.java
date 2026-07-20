package com.redo.domain.community.dto.res;

import java.util.List;

public record CommunityCommentListResponseDTO(
        List<CommunityCommentResponseDTO> comments
) {
}
