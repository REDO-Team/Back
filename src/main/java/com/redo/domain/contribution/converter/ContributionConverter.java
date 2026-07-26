package com.redo.domain.contribution.converter;

import com.redo.domain.contribution.dto.cache.ContributionEventCacheDTO;
import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.dto.res.OverallContributionResponseDTO;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionMilestoneStatus;
import com.redo.domain.user.entity.UserProfile;

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
        return ContributionMilestone.next(totalCertificationCount)
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

    public static OverallContributionResponseDTO toOverallContributionResponse(
            long totalParticipantCount,
            List<OverallContributionResponseDTO.FeedDTO> feeds,
            Long nextCursor,
            boolean hasNext
    ) {
        return new OverallContributionResponseDTO(
                totalParticipantCount,
                "현재 %d명의 사용자가 함께 지구를 지키고 있어요".formatted(totalParticipantCount),
                feeds,
                nextCursor,
                hasNext
        );
    }

    public static OverallContributionResponseDTO.FeedDTO toContributionFeed(
            ContributionEventCacheDTO event,
            UserProfile userProfile,
            String profileImageUrl
    ) {
        String nickname = userProfile == null ? "사용자" : userProfile.getNickname();

        return new OverallContributionResponseDTO.FeedDTO(
                event.eventId(),
                event.userId(),
                nickname,
                profileImageUrl,
                createFeedMessage(event, nickname),
                createHighlightText(event),
                event.eventType(),
                event.targetMilestone() == null
                        ? null
                        : event.targetMilestone().displayName(),
                event.showRemainingCount() ? event.remainingCount() : null,
                event.createdAt()
        );
    }

    private static String createFeedMessage(
            ContributionEventCacheDTO event,
            String nickname
    ) {
        return switch (event.eventType()) {
            case FIRST_CERTIFICATION ->
                    "%s님이 첫 분리수거를 실천했어요!".formatted(nickname);
            case DAILY_CERTIFICATION ->
                    "%s님이 %d번째 분리수거를 완료했어요!".formatted(
                            nickname,
                            event.certificationCount()
                    );
            case REWARD_PROGRESS -> createRewardProgressMessage(event, nickname);
        };
    }

    private static String createRewardProgressMessage(
            ContributionEventCacheDTO event,
            String nickname
    ) {
        ContributionMilestone targetMilestone = event.targetMilestone();
        if (event.showRemainingCount()) {
            return "%s님은 %s 제작까지 %d회 남았어요!".formatted(
                    nickname,
                    targetMilestone.displayName(),
                    event.remainingCount()
            );
        }

        return "%s님이 %s%s 만드는 중이에요!".formatted(
                nickname,
                targetMilestone.displayName(),
                targetMilestone.objectParticle()
        );
    }

    private static String createHighlightText(ContributionEventCacheDTO event) {
        return switch (event.eventType()) {
            case FIRST_CERTIFICATION -> "첫";
            case DAILY_CERTIFICATION -> "%d번째".formatted(event.certificationCount());
            case REWARD_PROGRESS -> event.showRemainingCount()
                    ? "%d회".formatted(event.remainingCount())
                    : event.targetMilestone().displayName();
        };
    }
}
