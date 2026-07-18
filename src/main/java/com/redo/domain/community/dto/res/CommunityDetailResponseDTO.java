package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityDetailResponseDTO(
        Long id,
        String title,
        String writer,
        String content,
        LocalDateTime createdAt,
        String imageUrl
) {
}
