package com.redo.domain.certification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationRetryIntake;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.entity.AiJudgement;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationJudgementMode;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.domain.certification.repository.AiJudgementRepository;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.certification.service.policy.CertificationPolicyEvaluator;
import com.redo.domain.certification.service.policy.CertificationPolicyResult;
import com.redo.domain.contribution.service.ContributionService;
import com.redo.domain.point.service.PointService;
import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.domain.recycleGuide.service.RecycleJudgementTemplateProvider;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationRetryTransactionServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long CERTIFICATION_ID = 101L;
    private static final Long GUIDE_ID = 12L;
    private static final String PREVIOUS_IMAGE_KEY =
            "certifications/42/previous.jpg";
    private static final String RETRY_IMAGE_KEY =
            "certifications/42/retry.jpg";
    private static final LocalDateTime PREVIOUS_JUDGED_AT =
            LocalDateTime.of(2026, 7, 31, 14, 3);

    @Mock
    private UserRepository userRepository;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private AiJudgementRepository aiJudgementRepository;
    @Mock
    private RecycleGuideRepository recycleGuideRepository;
    @Mock
    private RecycleJudgementTemplateProvider templateProvider;
    @Mock
    private CertificationPolicyEvaluator policyEvaluator;
    @Mock
    private PointService pointService;
    @Mock
    private ContributionService contributionService;
    @Mock
    private User user;
    @Mock
    private RecycleGuide guide;

    private CertificationTransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-31T05:08:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new CertificationTransactionService(
                userRepository,
                certificationRepository,
                aiJudgementRepository,
                recycleGuideRepository,
                templateProvider,
                policyEvaluator,
                pointService,
                contributionService,
                new ObjectMapper(),
                clock
        );
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(guide.getId()).thenReturn(GUIDE_ID);
        lenient().when(guide.getName()).thenReturn("투명 페트병");
    }

    @Test
    void startsRetryWithoutDailyOrCooldownRestrictionAndRestoresPreviousFailure() {
        Certification certification = retryableCertification();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(certificationRepository.findByIdAndUserIdForUpdate(
                CERTIFICATION_ID,
                USER_ID
        )).thenReturn(Optional.of(certification));
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(policy(
                CertificationRestrictionType.NONE,
                null
        ));
        when(templateProvider.findActiveByRecycleGuideId(GUIDE_ID))
                .thenReturn(Optional.of(template()));

        CertificationRetryIntake intake = service.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        );

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.PROCESSING);
        assertThat(certification.getImageKey()).isEqualTo(RETRY_IMAGE_KEY);
        assertThat(certification.getAttemptCount()).isEqualTo(2);
        assertThat(certification.getCertificationSource())
                .isEqualTo(CertificationSource.AFTER_SEARCH);
        assertThat(certification.getRewardPoint()).isEqualTo(100);
        assertThat(intake.command().mode()).isEqualTo(CertificationJudgementMode.RETRY);
        assertThat(intake.snapshot().imageKey()).isEqualTo(PREVIOUS_IMAGE_KEY);

        assertThat(service.restoreRetryIfProcessing(intake)).isTrue();

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.FAILED);
        assertThat(certification.getFailureType())
                .isEqualTo(CertificationFailureType.VLM_JUDGEMENT_FAILED);
        assertThat(certification.getImageKey()).isEqualTo(PREVIOUS_IMAGE_KEY);
        assertThat(certification.getJudgedAt()).isEqualTo(PREVIOUS_JUDGED_AT);
        assertThat(certification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void hidesMissingOrNotOwnedCertificationBehindNotFoundError() {
        when(certificationRepository.findByIdAndUserId(CERTIFICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateRetryPrerequisites(
                USER_ID,
                CERTIFICATION_ID
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.CERTIFICATION_NOT_FOUND);
    }

    @Test
    void rejectsRetryForAlreadyPassedCertification() {
        Certification certification = Certification.create(
                user,
                guide,
                PREVIOUS_IMAGE_KEY,
                CertificationSource.AFTER_SEARCH
        );
        certification.complete(AiJudgementResult.PASS, PREVIOUS_JUDGED_AT);
        when(certificationRepository.findByIdAndUserId(CERTIFICATION_ID, USER_ID))
                .thenReturn(Optional.of(certification));

        assertThatThrownBy(() -> service.validateRetryPrerequisites(
                USER_ID,
                CERTIFICATION_ID
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.PASSED_NOT_RETRYABLE);
    }

    /*
     * 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
     * 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
     * 기존 일일 한도 재촬영 차단 테스트를 원형 보존한다.
     *
    @Test
    void blocksRetryWhenDailyPassedLimitWasReached() {
        Certification certification = retryableCertification();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(certificationRepository.findByIdAndUserIdForUpdate(
                CERTIFICATION_ID,
                USER_ID
        )).thenReturn(Optional.of(certification));
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(policy(
                CertificationRestrictionType.DAILY_LIMIT_EXCEEDED,
                3L
        ));

        assertThatThrownBy(() -> service.startRetry(
                USER_ID,
                CERTIFICATION_ID,
                RETRY_IMAGE_KEY
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.DAILY_LIMIT_EXCEEDED);

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.FAILED);
        assertThat(certification.getAttemptCount()).isEqualTo(1);
    }
    */

    @Test
    void savesRetryPassWithoutSameGuideRestrictionAndEarnsOriginalSourcePoint() {
        Certification certification = processingRetryCertification();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(certificationRepository.findByIdAndUserIdForUpdate(
                CERTIFICATION_ID,
                USER_ID
        )).thenReturn(Optional.of(certification));
        CertificationCreateResponseDTO response = service.completeJudgement(
                retryContext(),
                passResult()
        );

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(response.earnedPoint()).isEqualTo(100);
        ArgumentCaptor<AiJudgement> judgementCaptor =
                ArgumentCaptor.forClass(AiJudgement.class);
        verify(aiJudgementRepository).save(judgementCaptor.capture());
        assertThat(judgementCaptor.getValue().getJudgementTemplateId()).isEqualTo(7L);
        InOrder completionOrder = inOrder(pointService, contributionService);
        completionOrder.verify(pointService).earnPoint(
                USER_ID,
                CERTIFICATION_ID,
                CertificationSource.AFTER_SEARCH,
                "certification:101:earn"
        );
        completionOrder.verify(contributionService)
                .recordPassedCertification(CERTIFICATION_ID);
    }

    /*
     * 데모데이 시현을 위해 동일 품목 일일 중복 제한을 비활성화함 (2026-08-21)
     * 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
     * 기존 재촬영 PASS 완료 직전 동일 가이드 정책 거절 테스트를 원형 보존한다.
     *
    @Test
    void returnsNonRetryableDuplicateWithoutSavingJudgementOrPoint() {
        Certification certification = processingRetryCertification();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(certificationRepository.findByIdAndUserIdForUpdate(
                CERTIFICATION_ID,
                USER_ID
        )).thenReturn(Optional.of(certification));
        when(certificationRepository
                .existsByUserIdAndRecycleGuideIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        any(), any(), any(), any(), any()
                )).thenReturn(true);

        CertificationCreateResponseDTO response = service.completeJudgement(
                retryContext(),
                passResult()
        );

        assertThat(response.status()).isEqualTo(CertificationStatus.FAILED);
        assertThat(response.failureType())
                .isEqualTo(CertificationFailureType.DUPLICATE_GUIDE_TODAY);
        assertThat(response.retryAllowed()).isFalse();
        verify(aiJudgementRepository, never()).save(any());
        verify(pointService, never()).earnPoint(any(), any(), any(), any());
        verify(contributionService, never()).recordPassedCertification(any());
    }
    */

    @Test
    void allowsRetryPassWithoutRecheckingDailyLimit() {
        Certification certification = processingRetryCertification();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(certificationRepository.findByIdAndUserIdForUpdate(
                CERTIFICATION_ID,
                USER_ID
        )).thenReturn(Optional.of(certification));
        CertificationCreateResponseDTO response = service.completeJudgement(
                retryContext(),
                passResult()
        );

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.PASSED);
        verify(certificationRepository, never())
                .countByUserIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        any(), any(), any(), any()
                );
        verify(pointService).earnPoint(
                USER_ID,
                CERTIFICATION_ID,
                CertificationSource.AFTER_SEARCH,
                "certification:101:earn"
        );
    }

    private Certification retryableCertification() {
        Certification certification = Certification.create(
                user,
                guide,
                PREVIOUS_IMAGE_KEY,
                CertificationSource.AFTER_SEARCH
        );
        ReflectionTestUtils.setField(certification, "id", CERTIFICATION_ID);
        certification.complete(AiJudgementResult.FAIL, PREVIOUS_JUDGED_AT);
        return certification;
    }

    private Certification processingRetryCertification() {
        Certification certification = retryableCertification();
        certification.retry(RETRY_IMAGE_KEY);
        return certification;
    }

    private CertificationJudgementContext retryContext() {
        return new CertificationJudgementContext(
                CERTIFICATION_ID,
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                "투명 페트병",
                "플라스틱",
                100,
                template(),
                CertificationJudgementMode.RETRY
        );
    }

    private CertificationVlmResult passResult() {
        return new CertificationVlmResult(
                AiJudgementResult.PASS,
                "분리배출 기준을 충족합니다.",
                List.of(),
                "{\"result\":\"PASS\"}"
        );
    }

    private CertificationPolicyResult policy(
            CertificationRestrictionType type,
            Long usedCount
    ) {
        return new CertificationPolicyResult(
                usedCount == null ? 0 : usedCount,
                type,
                null,
                0,
                null,
                null
        );
    }

    private ActiveRecycleJudgementTemplate template() {
        return new ActiveRecycleJudgementTemplate(
                7L,
                GUIDE_ID,
                1,
                "라벨을 제거했는지 확인",
                "{\"clean\":true}",
                "{\"contaminated\":true}",
                "세척 후 다시 촬영"
        );
    }
}
