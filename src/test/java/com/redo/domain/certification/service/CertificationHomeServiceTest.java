package com.redo.domain.certification.service;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.service.policy.CertificationPolicyEvaluator;
import com.redo.domain.certification.service.policy.CertificationPolicyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationHomeServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private CertificationPolicyEvaluator policyEvaluator;

    private CertificationHomeService certificationHomeService;

    @BeforeEach
    void setUp() {
        certificationHomeService = new CertificationHomeService(policyEvaluator);
    }

    @Test
    void mapsAvailablePolicyAndFixedRewardPointsToHomeResponse() {
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                1,
                CertificationRestrictionType.NONE,
                null,
                0,
                null,
                null
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.dailyLimit()).isEqualTo(3);
        assertThat(response.usedCount()).isEqualTo(1);
        assertThat(response.remainingCount()).isEqualTo(2);
        assertThat(response.canCertify()).isTrue();
        assertThat(response.restriction().type()).isEqualTo(CertificationRestrictionType.NONE);
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
    void mapsCooldownRecoveryFields() {
        LocalDateTime retryAvailableAt = LocalDateTime.of(2026, 7, 25, 10, 5);
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                1,
                CertificationRestrictionType.COOLDOWN,
                retryAvailableAt,
                121,
                null,
                null
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.canCertify()).isFalse();
        assertThat(response.restriction().retryAvailableAt()).isEqualTo(retryAvailableAt);
        assertThat(response.restriction().remainingSeconds()).isEqualTo(121);
    }

    @Test
    void mapsProcessingRecoveryFields() {
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                1,
                CertificationRestrictionType.PROCESSING_EXISTS,
                null,
                0,
                77L,
                "/api/certification/77/status"
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.canCertify()).isFalse();
        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.PROCESSING_EXISTS);
        assertThat(response.restriction().processingCertificationId()).isEqualTo(77L);
        assertThat(response.restriction().statusPath())
                .isEqualTo("/api/certification/77/status");
    }

    @Test
    void remainingCountNeverBecomesNegative() {
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                4,
                CertificationRestrictionType.DAILY_LIMIT_EXCEEDED,
                null,
                0,
                null,
                null
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.remainingCount()).isZero();
        assertThat(response.canCertify()).isFalse();
    }
}
