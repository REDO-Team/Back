package com.redo.domain.contribution.service;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.contribution.converter.ContributionConverter;
import com.redo.domain.contribution.dto.cache.ContributionEventCacheDTO;
import com.redo.domain.contribution.dto.res.MyContributionResponseDTO;
import com.redo.domain.contribution.dto.res.OverallContributionResponseDTO;
import com.redo.domain.contribution.entity.ContributionEvent;
import com.redo.domain.contribution.enums.ContributionMilestone;
import com.redo.domain.contribution.enums.ContributionType;
import com.redo.domain.contribution.exception.ContributionException;
import com.redo.domain.contribution.exception.code.ContributionErrorCode;
import com.redo.domain.contribution.repository.ContributionEventRepository;
import com.redo.domain.user.entity.UserProfile;
import com.redo.domain.user.enums.UserStatus;
import com.redo.domain.user.repository.UserProfileRepository;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContributionService {

    private final CertificationRepository certificationRepository;
    private final ContributionEventRepository contributionEventRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;
    private final ContributionFeedCacheService contributionFeedCacheService;
    private final S3Service s3Service;

    // 나의 기여도 조회 로직
    public MyContributionResponseDTO getMyContribution(Long userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ContributionException(
                        ContributionErrorCode.CONTRIBUTION_NOT_FOUND
                ));

        long totalCertificationCount = certificationRepository.countByUserIdAndStatus(
                userId,
                CertificationStatus.PASSED
        );

        return ContributionConverter.toMyContributionResponse(
                userProfile.getNickname(),
                totalCertificationCount
        );
    }

    // 전체 기여도 조회 로직
    public OverallContributionResponseDTO getOverallContribution(Long cursor, int size) {
        int querySize = size + 1;
        List<ContributionEventCacheDTO> events = getFeedEvents(cursor, querySize);
        boolean hasNext = events.size() > size;
        List<ContributionEventCacheDTO> pageEvents = hasNext
                ? events.subList(0, size)
                : events;

        Map<Long, UserProfile> userProfiles = getUserProfiles(pageEvents);
        Map<String, String> profileImageUrls = createProfileImageUrls(userProfiles.values());
        List<OverallContributionResponseDTO.FeedDTO> feeds = pageEvents.stream()
                .map(event -> {
                    UserProfile profile = userProfiles.get(event.userId());
                    String profileImageUrl = profile == null
                            ? null
                            : resolveProfileImageUrl(profile, profileImageUrls);
                    return ContributionConverter.toContributionFeed(
                            event,
                            profile,
                            profileImageUrl
                    );
                })
                .toList();

        Long nextCursor = hasNext && !pageEvents.isEmpty()
                ? pageEvents.get(pageEvents.size() - 1).eventId()
                : null;
        long participantCount = contributionFeedCacheService.getActiveParticipantCount(
                () -> userRepository.countByStatus(UserStatus.ACTIVE)
        );

        return ContributionConverter.toOverallContributionResponse(
                participantCount,
                feeds,
                nextCursor,
                hasNext
        );
    }

    /**
     * 인증 성공 처리 구현이 연결되면 PASSED 확정 트랜잭션에서 호출합니다.
     */
    @Transactional
    public ContributionEvent recordPassedCertification(Long certificationId) {
        return contributionEventRepository.findByCertificationId(certificationId)
                .orElseGet(() -> createContributionEvent(certificationId));
    }

    private ContributionEvent createContributionEvent(Long certificationId) {
        Certification certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new ContributionException(
                        ContributionErrorCode.CERTIFICATION_NOT_FOUND
                ));
        if (certification.getStatus() != CertificationStatus.PASSED) {
            throw new ContributionException(
                    ContributionErrorCode.INVALID_CERTIFICATION_STATUS
            );
        }

        long certificationCount = certificationRepository.countByUserIdAndStatus(
                certification.getUser().getId(),
                CertificationStatus.PASSED
        );
        ContributionMilestone nextMilestone = ContributionMilestone.next(certificationCount)
                .orElse(null);
        ContributionType eventType = resolveContributionType(
                certificationCount,
                nextMilestone
        );
        int remainingCount = nextMilestone == null
                ? 0
                : nextMilestone.requiredCount() - Math.toIntExact(certificationCount);
        boolean showRemainingCount = eventType == ContributionType.REWARD_PROGRESS
                && shouldShowRemainingCount(certificationCount, nextMilestone);

        try {
            return contributionEventRepository.saveAndFlush(
                    ContributionEvent.create(
                            certification.getUser(),
                            certification,
                            eventType,
                            certificationCount,
                            eventType == ContributionType.REWARD_PROGRESS ? nextMilestone : null,
                            eventType == ContributionType.REWARD_PROGRESS ? remainingCount : null,
                            showRemainingCount
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ContributionException(
                    ContributionErrorCode.DUPLICATE_CONTRIBUTION_EVENT
            );
        }
    }

    private List<ContributionEventCacheDTO> getFeedEvents(
            Long cursor,
            int querySize
    ) {
        List<Long> eventIds = contributionEventRepository.findFeedEventIds(
                cursor,
                UserStatus.ACTIVE,
                PageRequest.of(0, querySize)
        );
        if (eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ContributionEventCacheDTO> eventCache =
                new HashMap<>(contributionFeedCacheService.getFeedEvents(eventIds));
        List<Long> missingEventIds = eventIds.stream()
                .filter(eventId -> !eventCache.containsKey(eventId))
                .toList();
        cacheMissingFeedEvents(missingEventIds, eventCache);

        List<ContributionEventCacheDTO> events = new ArrayList<>();
        for (Long eventId : eventIds) {
            ContributionEventCacheDTO event = eventCache.get(eventId);
            if (event != null) {
                events.add(event);
            }
        }
        return events;
    }

    private void cacheMissingFeedEvents(
            List<Long> missingEventIds,
            Map<Long, ContributionEventCacheDTO> eventCache
    ) {
        if (missingEventIds.isEmpty()) {
            return;
        }

        List<ContributionEventCacheDTO> loadedEvents =
                contributionEventRepository.findFeedEventsByIdIn(
                                missingEventIds,
                                UserStatus.ACTIVE
                        ).stream()
                        .map(ContributionEventCacheDTO::from)
                        .toList();
        contributionFeedCacheService.putFeedEvents(loadedEvents);
        loadedEvents.forEach(event -> eventCache.put(event.eventId(), event));
    }

    private Map<Long, UserProfile> getUserProfiles(
            Collection<ContributionEventCacheDTO> events
    ) {
        List<Long> userIds = events.stream()
                .map(ContributionEventCacheDTO::userId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userProfileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        Function.identity()
                ));
    }

    private Map<String, String> createProfileImageUrls(
            Collection<UserProfile> userProfiles
    ) {
        return userProfiles.stream()
                .map(UserProfile::getProfileImageKey)
                .filter(imageKey -> imageKey != null && !imageKey.isBlank())
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        s3Service::createPresignedUrl
                ));
    }

    private String resolveProfileImageUrl(
            UserProfile userProfile,
            Map<String, String> profileImageUrls
    ) {
        String profileImageKey = userProfile.getProfileImageKey();
        if (profileImageKey == null || profileImageKey.isBlank()) {
            return userProfile.getCharacterCode();
        }
        return profileImageUrls.get(profileImageKey);
    }

    private ContributionType resolveContributionType(
            long certificationCount,
            ContributionMilestone nextMilestone
    ) {
        if (certificationCount == 1) {
            return ContributionType.FIRST_CERTIFICATION;
        }
        if (nextMilestone != null && ThreadLocalRandom.current().nextBoolean()) {
            return ContributionType.REWARD_PROGRESS;
        }
        return ContributionType.DAILY_CERTIFICATION;
    }

    private boolean shouldShowRemainingCount(
            long certificationCount,
            ContributionMilestone nextMilestone
    ) {
        int previousRequiredCount = Arrays.stream(ContributionMilestone.values())
                .filter(milestone -> milestone.requiredCount() <= certificationCount)
                .mapToInt(ContributionMilestone::requiredCount)
                .max()
                .orElse(0);
        int interval = nextMilestone.requiredCount() - previousRequiredCount;
        long remainingCount = nextMilestone.requiredCount() - certificationCount;
        return remainingCount * 2 <= interval;
    }
}
