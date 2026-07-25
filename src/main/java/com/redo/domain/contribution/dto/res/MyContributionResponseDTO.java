package com.redo.domain.contribution.dto.res;

import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionMilestoneStatus;

import java.util.List;

public record MyContributionResponseDTO(
        String nickname,
        long totalCertificationCount,
        String summaryMessage,
        MilestoneDTO latestAchievedMilestone,
        MilestoneDTO nextMilestone,
        long remainingCount,
        List<MilestoneDTO> milestones
) {

    public record MilestoneDTO(
            ContributionMilestone type,
            String name,
            int requiredCertificationCount,
            ContributionMilestoneStatus status
    ) {
    }
}
