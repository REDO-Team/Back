package com.redo.domain.certification.service.image;

public record CertificationImageContent(
        String mimeType,
        byte[] bytes
) {
}
