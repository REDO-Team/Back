package com.redo.domain.certification.dto;

public record CertificationRetryIntake(
        CertificationJudgementCommand command,
        CertificationRetrySnapshot snapshot,
        int attemptCount
) {
}
