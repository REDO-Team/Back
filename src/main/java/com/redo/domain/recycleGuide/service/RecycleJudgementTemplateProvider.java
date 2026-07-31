package com.redo.domain.recycleGuide.service;

import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;
import com.redo.domain.recycleGuide.entity.RecycleJudgementTemplate;
import com.redo.domain.recycleGuide.repository.RecycleJudgementTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleJudgementTemplateProvider {

    private final RecycleJudgementTemplateRepository repository;

    public Optional<ActiveRecycleJudgementTemplate> findActiveByRecycleGuideId(
            Long recycleGuideId
    ) {
        return repository
                .findTopByRecycleGuideIdAndIsActiveTrueOrderByVersionDesc(recycleGuideId)
                .map(this::toSnapshot);
    }

    private ActiveRecycleJudgementTemplate toSnapshot(
            RecycleJudgementTemplate template
    ) {
        return new ActiveRecycleJudgementTemplate(
                template.getId(),
                template.getRecycleGuide().getId(),
                template.getVersion(),
                template.getPromptTemplate(),
                template.getPassConditionsJson(),
                template.getFailConditionsJson(),
                template.getRetryGuideTemplate()
        );
    }
}
