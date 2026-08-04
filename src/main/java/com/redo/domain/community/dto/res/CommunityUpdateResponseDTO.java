package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityUpdateResponseDTO(
        Long id,
        String title,
        String content,
        String category,
        List<CommunityImageResponseDTO> images,
        LocalDateTime updatedAt
) {
}
