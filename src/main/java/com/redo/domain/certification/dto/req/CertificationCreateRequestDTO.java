package com.redo.domain.certification.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public record CertificationCreateRequestDTO(
        @Schema(description = "실시간 촬영한 인증 이미지", type = "string", format = "binary")
        MultipartFile image,

        @Schema(
                description = "인증 방식",
                allowableValues = {"GENERAL", "AFTER_SEARCH"},
                example = "GENERAL"
        )
        String certificationSource,

        @Schema(
                description = "검색 후 인증에서 선택한 DB 배출 가이드 ID. GENERAL에서는 생략",
                nullable = true,
                example = "12"
        )
        Long recycleGuideId
) {
}
