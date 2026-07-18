package com.redo.domain.community.dto.req;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CommunityCreateRequestDTO(
        Integer category,
        String title,
        String content,
        List<MultipartFile> image
) {
}
