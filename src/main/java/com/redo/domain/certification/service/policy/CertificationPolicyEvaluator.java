package com.redo.domain.certification.service.policy;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.redo.domain.certification.config.CertificationTimeConfig.CERTIFICATION_CLOCK;
import static com.redo.domain.certification.config.CertificationTimeConfig.SEOUL_ZONE;

@Component
public class CertificationPolicyEvaluator {

    public static final int DAILY_LIMIT = 3;
    public static final long COOLDOWN_SECONDS = 300;
    public static final int SAME_GUIDE_DAILY_LIMIT = 1;
    public static final boolean LIVE_CAPTURE_ONLY = true;

    private static final List<CertificationStatus> COMPLETED_STATUSES =
            List.of(CertificationStatus.PASSED, CertificationStatus.FAILED);

    private final CertificationRepository certificationRepository;
    private final Clock clock;

    public CertificationPolicyEvaluator(
            CertificationRepository certificationRepository,
            @Qualifier(CERTIFICATION_CLOCK) Clock clock
    ) {
        this.certificationRepository = certificationRepository;
        this.clock = clock;
    }

    public CertificationPolicyResult evaluate(Long userId) {
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
        if (usedCount >= DAILY_LIMIT) {
            return empty(usedCount, CertificationRestrictionType.DAILY_LIMIT_EXCEEDED);
        }

        Optional<Certification> processingCertification =
                certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        CertificationStatus.PROCESSING
                );
        if (processingCertification.isPresent()) {
            Long certificationId = processingCertification.get().getId();
            return new CertificationPolicyResult(
                    usedCount,
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
            return empty(usedCount, CertificationRestrictionType.NONE);
        }

        LocalDateTime retryAvailableAt =
                recentCompleted.get().getJudgedAt().plusSeconds(COOLDOWN_SECONDS);
        if (!now.isBefore(retryAvailableAt)) {
            return empty(usedCount, CertificationRestrictionType.NONE);
        }

        return new CertificationPolicyResult(
                usedCount,
                CertificationRestrictionType.COOLDOWN,
                retryAvailableAt,
                ceilPositiveSeconds(Duration.between(now, retryAvailableAt)),
                null,
                null
        );
    }

    private CertificationPolicyResult empty(
            long usedCount,
            CertificationRestrictionType type
    ) {
        return new CertificationPolicyResult(
                usedCount,
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
