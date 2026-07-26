package com.redo.domain.contribution.dto.cache;

import com.redo.domain.contribution.entity.ContributionEvent;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionType;

import java.time.LocalDateTime;

public record ContributionEventCacheDTO(
        Long eventId,
        Long userId,
        ContributionType eventType,
        long certificationCount,
        ContributionMilestone targetMilestone,
        Integer remainingCount,
        boolean showRemainingCount,
        LocalDateTime createdAt
) {

    public static ContributionEventCacheDTO from(ContributionEvent event) {
        return new ContributionEventCacheDTO(
                event.getId(),
                event.getUser().getId(),
                event.getEventType(),
                event.getCertificationCount(),
                event.getTargetMilestone(),
                event.getRemainingCount(),
                event.isShowRemainingCount(),
                event.getCreatedAt()
        );
    }
}
