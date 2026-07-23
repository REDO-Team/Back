package com.redo.domain.certification.service;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationHomeServiceTest {

    private static final Long USER_ID = 42L;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-22T04:05:00Z"), SEOUL);
    private static final LocalDateTime TODAY_START =
            LocalDateTime.of(2026, 7, 22, 0, 0);
    private static final LocalDateTime TOMORROW_START =
            LocalDateTime.of(2026, 7, 23, 0, 0);

    @Mock
    private CertificationRepository certificationRepository;

    private CertificationHomeService certificationHomeService;

    @BeforeEach
    void setUp() {
        certificationHomeService =
                new CertificationHomeService(certificationRepository, FIXED_CLOCK);
    }

    @Test
    void returnsAvailableHomeWhenUserHasNoCertificationHistory() {
        stubUsedCount(0);
        when(certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                CertificationStatus.PROCESSING
        )).thenReturn(Optional.empty());
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        List.of(CertificationStatus.PASSED, CertificationStatus.FAILED)
                )).thenReturn(Optional.empty());

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.dailyLimit()).isEqualTo(3);
        assertThat(response.usedCount()).isZero();
        assertThat(response.remainingCount()).isEqualTo(3);
        assertThat(response.canCertify()).isTrue();
        assertThat(response.restriction().type()).isEqualTo(CertificationRestrictionType.NONE);
        assertThat(response.restriction().remainingSeconds()).isZero();
        assertThat(response.policy().cooldownSeconds()).isEqualTo(300);
        assertThat(response.policy().sameGuideDailyLimit()).isEqualTo(1);
        assertThat(response.policy().liveCaptureOnly()).isTrue();
        assertThat(response.rewardPolicy().generalCertificationPoint())
                .isEqualTo(CertificationSource.GENERAL.rewardPoint())
                .isEqualTo(50);
        assertThat(response.rewardPolicy().afterSearchCertificationPoint())
                .isEqualTo(CertificationSource.AFTER_SEARCH.rewardPoint())
                .isEqualTo(100);
    }

    @Test
    void dailyLimitHasHighestPriorityAndRemainingCountNeverBecomesNegative() {
        stubUsedCount(4);

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.usedCount()).isEqualTo(4);
        assertThat(response.remainingCount()).isZero();
        assertThat(response.canCertify()).isFalse();
        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.DAILY_LIMIT_EXCEEDED);
        verifyNoFurtherPolicyQueries();
    }

    @Test
    void returnsLatestProcessingCertificationRecoveryPath() {
        stubUsedCount(1);
        Certification processing = mock(Certification.class);
        when(processing.getId()).thenReturn(101L);
        when(certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                CertificationStatus.PROCESSING
        )).thenReturn(Optional.of(processing));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.canCertify()).isFalse();
        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.PROCESSING_EXISTS);
        assertThat(response.restriction().processingCertificationId()).isEqualTo(101L);
        assertThat(response.restriction().statusPath())
                .isEqualTo("/api/certification/101/status");
        assertThat(response.restriction().retryAvailableAt()).isNull();
        verifyNoRecentCompletedQuery();
    }

    @Test
    void processingCertificationTakesPriorityOverCooldown() {
        stubUsedCount(1);
        Certification processing = mock(Certification.class);
        when(processing.getId()).thenReturn(102L);
        when(certificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                USER_ID,
                CertificationStatus.PROCESSING
        )).thenReturn(Optional.of(processing));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.PROCESSING_EXISTS);
        verifyNoRecentCompletedQuery();
    }

    @Test
    void cooldownRoundsRemainingSecondsUp() {
        stubUsedCount(1);
        stubNoProcessing();
        Certification completed = mock(Certification.class);
        when(completed.getJudgedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 22, 13, 3, 2, 500_000_000));
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        List.of(CertificationStatus.PASSED, CertificationStatus.FAILED)
                )).thenReturn(Optional.of(completed));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.canCertify()).isFalse();
        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.COOLDOWN);
        assertThat(response.restriction().retryAvailableAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 22, 13, 8, 2, 500_000_000));
        assertThat(response.restriction().remainingSeconds()).isEqualTo(183);
    }

    @Test
    void cooldownEndsExactlyFiveMinutesAfterCompletion() {
        stubUsedCount(1);
        stubNoProcessing();
        Certification completed = mock(Certification.class);
        when(completed.getJudgedAt())
                .thenReturn(LocalDateTime.of(2026, 7, 22, 13, 0));
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        List.of(CertificationStatus.PASSED, CertificationStatus.FAILED)
                )).thenReturn(Optional.of(completed));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.canCertify()).isTrue();
        assertThat(response.restriction().type()).isEqualTo(CertificationRestrictionType.NONE);
    }

    @Test
    void calculatesTodayUsingSeoulDateBoundary() {
        Clock midnightInSeoul =
                Clock.fixed(Instant.parse("2026-07-21T15:00:00Z"), SEOUL);
        certificationHomeService =
                new CertificationHomeService(certificationRepository, midnightInSeoul);
        stubUsedCount(0);
        stubNoProcessing();
        when(certificationRepository
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        List.of(CertificationStatus.PASSED, CertificationStatus.FAILED)
                )).thenReturn(Optional.empty());

        certificationHomeService.getHome(USER_ID);

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

    private void verifyNoFurtherPolicyQueries() {
        verifyNoInteractionsAfterCount();
    }

    private void verifyNoInteractionsAfterCount() {
        verify(certificationRepository)
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        USER_ID,
                        CertificationStatus.PASSED,
                        TODAY_START,
                        TOMORROW_START
                );
        verifyNoRecentCompletedQuery();
        verify(certificationRepository, org.mockito.Mockito.never())
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        USER_ID,
                        CertificationStatus.PROCESSING
                );
    }

    private void verifyNoRecentCompletedQuery() {
        verify(certificationRepository, org.mockito.Mockito.never())
                .findTopByUserIdAndStatusInAndJudgedAtIsNotNullOrderByJudgedAtDesc(
                        USER_ID,
                        List.of(CertificationStatus.PASSED, CertificationStatus.FAILED)
                );
    }
}
