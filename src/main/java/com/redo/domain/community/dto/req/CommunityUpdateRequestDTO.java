package com.redo.domain.community.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 첨부 이미지는 부분 수정한다. deleteImageIds 로 지정한 기존 이미지만 삭제하고, images 로 받은 이미지를 새로 추가한다.
// (둘 다 보내지 않으면 기존 이미지는 그대로 유지된다.)
public record CommunityUpdateRequestDTO(
        @NotNull Integer category,
        @NotBlank String title,
        @NotBlank String content,
        List<Long> deleteImageIds,
        List<MultipartFile> images
) {
}
