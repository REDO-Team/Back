package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityCommentResponseDTO(
        Long commentId,
        String writer,
        String profileImageUrl,
        String characterCode,
        String content,
        LocalDateTime createdAt
) {
}
