package com.redo.domain.community.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityDetailResponseDTO(
        Long id,
        String title,
        String writer,
        String profileImageUrl,
        String characterCode,
        String content,
        LocalDateTime createdAt,
        // imageUrls 는 기존 클라이언트 호환을 위해 유지하고, 수정 시 삭제할 이미지를 지정할 수 있도록 images(id + url)를 함께 내려준다.
        List<String> imageUrls,
        List<CommunityImageResponseDTO> images,
        String category,
        Long numComments,
        Integer numLikes,
        // ApiResponse 의 isSuccess 와 동일하게, Jackson 이 is 접두사를 떼지 않도록 이름을 고정한다.
        @JsonProperty("isLiked") boolean isLiked,
        @JsonProperty("isMine") boolean isMine
) {
}
