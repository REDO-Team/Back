package com.redo.domain.contribution.converter;

import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionMilestoneStatus;

import java.util.Arrays;
import java.util.List;

public class ContributionConverter {

    private ContributionConverter() {
    }

    public static MyContributionResponseDTO toMyContributionResponse(
            String nickname,
            long totalCertificationCount
    ) {
        List<MyContributionResponseDTO.MilestoneDTO> milestones = Arrays.stream(ContributionMilestone.values())
                .map(type -> toMilestone(type, totalCertificationCount))
                .toList();

        MyContributionResponseDTO.MilestoneDTO latestAchievedMilestone = milestones.stream()
                .filter(milestone -> milestone.status() == ContributionMilestoneStatus.ACHIEVED)
                .reduce((previous, current) -> current)
                .orElse(null);

        MyContributionResponseDTO.MilestoneDTO nextMilestone = milestones.stream()
                .filter(milestone -> milestone.status() == ContributionMilestoneStatus.IN_PROGRESS)
                .findFirst()
                .orElse(null);

        long remainingCount = nextMilestone == null
                ? 0
                : nextMilestone.requiredCertificationCount() - totalCertificationCount;

        return new MyContributionResponseDTO(
                nickname,
                totalCertificationCount,
                createSummaryMessage(
                        nickname,
                        totalCertificationCount,
                        latestAchievedMilestone,
                        nextMilestone,
                        remainingCount
                ),
                latestAchievedMilestone,
                nextMilestone,
                remainingCount,
                milestones
        );
    }

    private static MyContributionResponseDTO.MilestoneDTO toMilestone(
            ContributionMilestone type,
            long totalCertificationCount
    ) {
        ContributionMilestoneStatus status;
        if (totalCertificationCount >= type.requiredCount()) {
            status = ContributionMilestoneStatus.ACHIEVED;
        } else if (isNextMilestone(type, totalCertificationCount)) {
            status = ContributionMilestoneStatus.IN_PROGRESS;
        } else {
            status = ContributionMilestoneStatus.LOCKED;
        }

        return new MyContributionResponseDTO.MilestoneDTO(
                type,
                type.displayName(),
                type.requiredCount(),
                status
        );
    }

    private static boolean isNextMilestone(
            ContributionMilestone type,
            long totalCertificationCount
    ) {
        return Arrays.stream(ContributionMilestone.values())
                .filter(candidate -> totalCertificationCount < candidate.requiredCount())
                .findFirst()
                .map(candidate -> candidate == type)
                .orElse(false);
    }

    private static String createSummaryMessage(
            String nickname,
            long totalCertificationCount,
            MyContributionResponseDTO.MilestoneDTO latestAchievedMilestone,
            MyContributionResponseDTO.MilestoneDTO nextMilestone,
            long remainingCount
    ) {
        if (latestAchievedMilestone != null) {
            ContributionMilestone achievedType = latestAchievedMilestone.type();
            return "%s님! 지금까지 %d번의 분리수거로 %s%s 만들었어요!".formatted(
                    nickname,
                    totalCertificationCount,
                    achievedType.displayName(),
                    achievedType.objectParticle()
            );
        }

        return "%s님! 첫 번째 재활용 물품까지 %d회 남았어요!".formatted(
                nickname,
                nextMilestone == null ? 0 : remainingCount
        );
    }
}
