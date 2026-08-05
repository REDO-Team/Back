package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityCommentUpdateResponseDTO(
        Long commentId,
        String content,
        LocalDateTime updatedAt
) {
}
