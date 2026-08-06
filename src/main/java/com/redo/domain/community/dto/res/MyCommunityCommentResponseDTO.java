package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

// 내가 작성한 댓글 목록의 단건 응답
// 목록에서 바로 원본 게시글로 이동할 수 있도록 댓글이 달린 게시글 정보를 함께 담는다.
public record MyCommunityCommentResponseDTO(
        Long commentId,
        String content,
        LocalDateTime createdAt,
        Long communityId,
        String communityTitle,
        String communityCategory,
        String communityImageUrl
) {
}
