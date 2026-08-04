package com.redo.domain.community.dto.res;

// 수정 시 삭제할 이미지를 지정할 수 있도록 조회용 Presigned URL 과 이미지 id 를 함께 내려준다.
public record CommunityImageResponseDTO(
        Long imageId,
        String imageUrl
) {
}
