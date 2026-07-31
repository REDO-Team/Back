package com.redo.domain.certification.dto;

import com.redo.domain.certification.enums.CertificationJudgementMode;
import com.redo.domain.certification.enums.CertificationSource;

public record CertificationJudgementCommand(
        Long certificationId,
        Long userId,
        CertificationSource source,
        String imageKey,
        Long recycleGuideId,
        CertificationJudgementMode mode
) {

    public CertificationJudgementCommand(
            Long certificationId,
            Long userId,
            CertificationSource source,
            String imageKey,
            Long recycleGuideId
    ) {
        this(
                certificationId,
                userId,
                source,
                imageKey,
                recycleGuideId,
                CertificationJudgementMode.forCreate(source)
        );
    }

    public static CertificationJudgementCommand retry(
            Long certificationId,
            Long userId,
            CertificationSource source,
            String imageKey,
            Long recycleGuideId
    ) {
        return new CertificationJudgementCommand(
                certificationId,
                userId,
                source,
                imageKey,
                recycleGuideId,
                CertificationJudgementMode.RETRY
        );
    }
}
