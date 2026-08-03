package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityResponseDTO(
        Long id,
        Long numComments,
        Integer numLikes,
        LocalDateTime createdAt,
        String imageUrl,
        String title,
        String category,
        String preview,
        String writer,
        String profileImageUrl,
        String characterCode
) {
}
