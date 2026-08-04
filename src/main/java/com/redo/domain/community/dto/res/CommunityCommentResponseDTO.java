package com.redo.domain.community.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record CommunityCommentResponseDTO(
        Long commentId,
        String writer,
        String profileImageUrl,
        String characterCode,
        String content,
        LocalDateTime createdAt,
        // ApiResponse 의 isSuccess 와 동일하게, Jackson 이 is 접두사를 떼지 않도록 이름을 고정한다.
        @JsonProperty("isMine") boolean isMine
) {
}
