package com.redo.domain.certification.service;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.redo.domain.certification.config.CertificationTimeConfig.CERTIFICATION_CLOCK;
import static com.redo.domain.certification.config.CertificationTimeConfig.SEOUL_ZONE;

@Service
@Transactional(readOnly = true)
public class CertificationHomeService {

    private static final int DAILY_LIMIT = 3;
    private static final long COOLDOWN_SECONDS = 300;
    private static final int SAME_GUIDE_DAILY_LIMIT = 1;
    private static final boolean LIVE_CAPTURE_ONLY = true;
    private static final List<CertificationStatus> COMPLETED_STATUSES =
            List.of(CertificationStatus.PASSED, CertificationStatus.FAILED);

    private final CertificationRepository certificationRepository;
    private final Clock clock;

    public CertificationHomeService(
            CertificationRepository certificationRepository,
            @Qualifier(CERTIFICATION_CLOCK) Clock clock
    ) {
        this.certificationRepository = certificationRepository;
        this.clock = clock;
    }

    public CertificationHomeResponseDTO getHome(Long userId) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), SEOUL_ZONE);
        LocalDate today = now.toLocalDate();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        long usedCount = certificationRepository
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        userId,
                        CertificationStatus.PASSED,
                        startAt,
                        endAt
                );
        int remainingCount = (int) Math.max(0L, DAILY_LIMIT - usedCount);

        CertificationHomeResponseDTO.RestrictionDTO restriction =
                resolveRestriction(userId, usedCount, now);

        return new CertificationHomeResponseDTO(
                DAILY_LIMIT,
                remainingCount,
                usedCount,
                restriction.type() == CertificationRestrictionType.NONE,
                restriction,
                new CertificationHomeResponseDTO.PolicyDTO(
                        COOLDOWN_SECONDS,
                        SAME_GUIDE_DAILY_LIMIT,
                        LIVE_CAPTURE_ONLY
                ),
                new CertificationHomeResponseDTO.RewardPolicyDTO(
                        CertificationSource.GENERAL.rewardPoint(),
                        CertificationSource.AFTER_SEARCH.rewardPoint()
                )
        );
    }

    private CertificationHomeResponseDTO.RestrictionDTO resolveRestriction(
            Long userId,
            long usedCount,
            LocalDateTime now
    ) {
        if (usedCount >= DAILY_LIMIT) {
            return emptyRestriction(CertificationRestrictionType.DAILY_LIMIT_EXCEEDED);
        }

        Optional<Certification> processingCertification =
                certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        CertificationStatus.PROCESSING
                );
        if (processingCertification.isPresent()) {
            Long certificationId = processingCertification.get().getId();
            return new CertificationHomeResponseDTO.RestrictionDTO(
                    CertificationRestrictionType.PROCESSING_EXISTS,
                    null,
                    0,
                    certificationId,
                    "/api/certification/" + certificationId + "/status"
            );
        }

        Optional<Certification> recentCompleted =
                certificationRepository
                        .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                                userId,
                                COMPLETED_STATUSES
                        );
        if (recentCompleted.isEmpty()) {
            return emptyRestriction(CertificationRestrictionType.NONE);
        }

        LocalDateTime retryAvailableAt =
                recentCompleted.get().getJudgedAt().plusSeconds(COOLDOWN_SECONDS);
        if (!now.isBefore(retryAvailableAt)) {
            return emptyRestriction(CertificationRestrictionType.NONE);
        }

        return new CertificationHomeResponseDTO.RestrictionDTO(
                CertificationRestrictionType.COOLDOWN,
                retryAvailableAt,
                ceilPositiveSeconds(Duration.between(now, retryAvailableAt)),
                null,
                null
        );
    }

    private CertificationHomeResponseDTO.RestrictionDTO emptyRestriction(
            CertificationRestrictionType type
    ) {
        return new CertificationHomeResponseDTO.RestrictionDTO(
                type,
                null,
                0,
                null,
                null
        );
    }

    private long ceilPositiveSeconds(Duration duration) {
        long seconds = duration.getSeconds();
        return duration.getNano() == 0 ? seconds : seconds + 1;
    }
}
