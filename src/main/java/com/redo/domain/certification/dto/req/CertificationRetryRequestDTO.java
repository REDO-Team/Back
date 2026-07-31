package com.redo.domain.certification.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

public record CertificationRetryRequestDTO(
        @Schema(description = "실시간 재촬영한 인증 이미지", type = "string", format = "binary")
        MultipartFile image
) {
}
