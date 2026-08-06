package com.redo.domain.community.dto.res;

import java.time.LocalDateTime;

// 내가 작성한 게시글 목록의 단건 응답
// 조회자 본인이 작성자인 목록이므로 작성자 프로필(닉네임/프로필 이미지/캐릭터 코드)은 담지 않는다.
public record MyCommunityResponseDTO(
        Long communityId,
        String title,
        String preview,
        String category,
        String imageUrl,
        Long numComments,
        Integer numLikes,
        LocalDateTime createdAt
) {
}
