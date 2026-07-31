package com.redo.domain.recycleGuide.dto;

public record ActiveRecycleJudgementTemplate(
        Long id,
        Long recycleGuideId,
        Integer version,
        String promptTemplate,
        String passConditionsJson,
        String failConditionsJson,
        String retryGuideTemplate
) {
}
