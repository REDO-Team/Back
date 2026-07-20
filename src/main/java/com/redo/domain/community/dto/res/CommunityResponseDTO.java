package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

public record CommunityResponseDTO(
        Long id,
        String category,
        String title,
        String imageUrl,
        Long numComments,
        LocalDateTime createdAt
) {
}
