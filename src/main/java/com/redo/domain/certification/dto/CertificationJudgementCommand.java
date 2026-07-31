package com.redo.domain.certification.dto;

import com.redo.domain.certification.enums.CertificationSource;

public record CertificationJudgementCommand(
        Long certificationId,
        Long userId,
        CertificationSource source,
        String imageKey,
        Long recycleGuideId
) {
}
