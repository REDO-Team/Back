package com.redo.domain.certification.service.policy;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.CertificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationPolicyEvaluatorTest {

    private static final Long USER_ID = 42L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-22T04:05:00Z"), SEOUL);
    private static final LocalDateTime TODAY_START =
            LocalDateTime.of(2026, 7, 22, 0, 0);
    private static final LocalDateTime TOMORROW_START =
            LocalDateTime.of(2026, 7, 23, 0, 0);
    private static final List<CertificationStatus> COMPLETED =
            List.of(CertificationStatus.PASSED, CertificationStatus.FAILED);

    @Mock
    private CertificationRepository certificationRepository;

    private CertificationPolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CertificationPolicyEvaluator(
                certificationRepository,
                FIXED_CLOCK
        );
    }

    @Test
    void returnsNoneWhenThereIsNoHistory() {
        stubUsedCount(0);
        stubNoProcessing();
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        COMPLETED
                )).thenReturn(Optional.empty());

        CertificationPolicyResult result = evaluator.evaluate(USER_ID);

        assertThat(result.type()).isEqualTo(CertificationRestrictionType.NONE);
        assertThat(result.usedCount()).isZero();
    }

    @Test
    void dailyLimitHasHighestPriority() {
        stubUsedCount(3);

        CertificationPolicyResult result = evaluator.evaluate(USER_ID);

        assertThat(result.type())
                .isEqualTo(CertificationRestrictionType.DAILY_LIMIT_EXCEEDED);
        verify(certificationRepository, never())
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        USER_ID,
                        CertificationStatus.PROCESSING
                );
    }

    @Test
    void processingHasPriorityOverCooldownAndProvidesRecoveryPath() {
        stubUsedCount(1);
        Certification processing = mock(Certification.class);
        when(processing.getId()).thenReturn(101L);
        when(certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                CertificationStatus.PROCESSING
        )).thenReturn(Optional.of(processing));

        CertificationPolicyResult result = evaluator.evaluate(USER_ID);

        assertThat(result.type())
                .isEqualTo(CertificationRestrictionType.PROCESSING_EXISTS);
        assertThat(result.processingCertificationId()).isEqualTo(101L);
        assertThat(result.statusPath()).isEqualTo("/api/certification/101/status");
        verify(certificationRepository, never())
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        COMPLETED
                );
    }

    @Test
    void cooldownUsesLatestPassedOrFailedAndRoundsUpSeconds() {
        stubUsedCount(1);
        stubNoProcessing();
        Certification completed = mock(Certification.class);
        when(completed.getJudgedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 22, 13, 3, 2, 500_000_000));
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        COMPLETED
                )).thenReturn(Optional.of(completed));

        CertificationPolicyResult result = evaluator.evaluate(USER_ID);

        assertThat(result.type()).isEqualTo(CertificationRestrictionType.COOLDOWN);
        assertThat(result.retryAvailableAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 22, 13, 8, 2, 500_000_000));
        assertThat(result.remainingSeconds()).isEqualTo(183);
    }

    @Test
    void cooldownEndsAtExactlyFiveMinutes() {
        stubUsedCount(1);
        stubNoProcessing();
        Certification completed = mock(Certification.class);
        when(completed.getJudgedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 22, 13, 0));
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        COMPLETED
                )).thenReturn(Optional.of(completed));

        CertificationPolicyResult result = evaluator.evaluate(USER_ID);

        assertThat(result.type()).isEqualTo(CertificationRestrictionType.NONE);
    }

    @Test
    void calculatesTodayUsingSeoulDateBoundary() {
        Clock midnightInSeoul =
                Clock.fixed(Instant.parse("2026-07-21T15:00:00Z"), SEOUL);
        evaluator = new CertificationPolicyEvaluator(
                certificationRepository,
                midnightInSeoul
        );
        stubUsedCount(0);
        stubNoProcessing();
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        COMPLETED
                )).thenReturn(Optional.empty());

        evaluator.evaluate(USER_ID);

        verify(certificationRepository)
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        USER_ID,
                        CertificationStatus.PASSED,
                        TODAY_START,
                        TOMORROW_START
                );
    }

    private void stubUsedCount(long usedCount) {
        when(certificationRepository
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        USER_ID,
                        CertificationStatus.PASSED,
                        TODAY_START,
                        TOMORROW_START
                )).thenReturn(usedCount);
    }

    private void stubNoProcessing() {
        when(certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                CertificationStatus.PROCESSING
        )).thenReturn(Optional.empty());
    }
}
