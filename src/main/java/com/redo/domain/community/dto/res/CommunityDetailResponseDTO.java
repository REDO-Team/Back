package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityDetailResponseDTO(
        Long id,
        String title,
        String writer,
        String profileImageUrl,
        String characterCode,
        String content,
        LocalDateTime createdAt,
        String imageUrl
) {
}
