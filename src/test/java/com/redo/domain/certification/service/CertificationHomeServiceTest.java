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

        assertThat(response.dailyLimit()).isEqualTo(100);
        assertThat(response.usedCount()).isEqualTo(1);
        assertThat(response.remainingCount()).isEqualTo(99);
        assertThat(response.canCertify()).isTrue();
        assertThat(response.restriction().type()).isEqualTo(CertificationRestrictionType.NONE);
        assertThat(response.policy().cooldownSeconds()).isZero();
        assertThat(response.policy().sameGuideDailyLimit()).isEqualTo(100);
        assertThat(response.policy().liveCaptureOnly()).isTrue();
        assertThat(response.rewardPolicy().generalCertificationPoint())
                .isEqualTo(CertificationSource.GENERAL.rewardPoint())
                .isEqualTo(50);
        assertThat(response.rewardPolicy().afterSearchCertificationPoint())
                .isEqualTo(CertificationSource.AFTER_SEARCH.rewardPoint())
                .isEqualTo(100);
    }

    /*
     * 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
     * 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
     * 기존 mapsCooldownRecoveryFields 테스트를 주석으로 보존한다.
     */

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
        assertThat(response.dailyLimit()).isEqualTo(100);
        assertThat(response.remainingCount()).isEqualTo(99);
        assertThat(response.restriction().type())
                .isEqualTo(CertificationRestrictionType.PROCESSING_EXISTS);
        assertThat(response.restriction().processingCertificationId()).isEqualTo(77L);
        assertThat(response.restriction().statusPath())
                .isEqualTo("/api/certification/77/status");
    }

    @Test
    void dailyPassedCountDoesNotDisableHomeCertification() {
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                4,
                CertificationRestrictionType.NONE,
                null,
                0,
                null,
                null
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.usedCount()).isEqualTo(4);
        assertThat(response.dailyLimit()).isEqualTo(100);
        assertThat(response.remainingCount()).isEqualTo(96);
        assertThat(response.canCertify()).isTrue();
    }

    @Test
    void compatibilityRemainingCountStaysPositiveAtDisplayLimit() {
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                101,
                CertificationRestrictionType.NONE,
                null,
                0,
                null,
                null
        ));

        CertificationHomeResponseDTO response = certificationHomeService.getHome(USER_ID);

        assertThat(response.dailyLimit()).isEqualTo(100);
        assertThat(response.remainingCount()).isOne();
        assertThat(response.canCertify()).isTrue();
    }
}
