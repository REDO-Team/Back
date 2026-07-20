package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityCreateResponseDTO(
        Long id,
        String title,
        String content,
        List<String> imageKeys,
        String writer,
        LocalDateTime createdAt
) {
}
