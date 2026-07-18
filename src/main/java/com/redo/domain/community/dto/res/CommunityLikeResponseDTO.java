package com.redo.domain.community.dto.res;

public record CommunityLikeResponseDTO(
        Long userId,
        Integer likeCount
) {
}
