package com.redo.domain.community.dto.req;

import jakarta.validation.constraints.NotBlank;

public record CommunityCommentUpdateRequestDTO(
        @NotBlank String comment
) {
}
