package com.redo.domain.certification.dto;

import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;

public record CertificationJudgementContext(
        Long certificationId,
        Long userId,
        CertificationSource source,
        Long recycleGuideId,
        String itemName,
        String categoryName,
        int rewardPoint,
        ActiveRecycleJudgementTemplate template
) {
}
