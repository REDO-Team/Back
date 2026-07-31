package com.redo.domain.recycleGuide.repository;

import com.redo.domain.recycleGuide.entity.RecycleJudgementTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecycleJudgementTemplateRepository
        extends JpaRepository<RecycleJudgementTemplate, Long> {

    Optional<RecycleJudgementTemplate>
    findTopByRecycleGuideIdAndIsActiveTrueOrderByVersionDesc(Long recycleGuideId);
}
