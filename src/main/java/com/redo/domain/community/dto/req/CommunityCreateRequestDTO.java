package com.redo.domain.community.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record CommunityCreateRequestDTO(
        @NotNull Integer category,
        @NotBlank String title,
        @NotBlank String content,
        List<MultipartFile> images
) {
}
