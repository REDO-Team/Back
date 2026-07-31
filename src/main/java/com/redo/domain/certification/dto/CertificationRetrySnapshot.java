package com.redo.domain.certification.dto;

import java.time.LocalDateTime;

public record CertificationRetrySnapshot(
        String imageKey,
        LocalDateTime judgedAt,
        int attemptCount
) {
}
