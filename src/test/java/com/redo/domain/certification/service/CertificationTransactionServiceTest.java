package com.redo.domain.certification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationJudgementPreparation;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.entity.AiJudgement;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationFailureType;
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
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificationTransactionServiceTest {

    private static final Long USER_ID = 42L;
    private static final Long GUIDE_ID = 12L;
    private static final Instant NOW = Instant.parse("2026-07-31T05:03:00Z");

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

    private CertificationTransactionService service;
    private User user;
    private RecycleGuide guide;

    @BeforeEach
    void setUp() {
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
                Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))
        );
        user = User.createGeneral("cert-user", "cert@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        guide = org.mockito.Mockito.mock(RecycleGuide.class);
        lenient().when(guide.getId()).thenReturn(GUIDE_ID);
        lenient().when(guide.getName()).thenReturn("투명 페트병");
    }

    @Test
    void createsAfterSearchProcessingSnapshotWithFixedReward() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(allowedPolicy());
        when(recycleGuideRepository.findById(GUIDE_ID)).thenReturn(Optional.of(guide));
        when(templateProvider.findActiveByRecycleGuideId(GUIDE_ID))
                .thenReturn(Optional.of(template()));
        when(certificationRepository.saveAndFlush(any(Certification.class)))
                .thenAnswer(invocation -> {
                    Certification certification = invocation.getArgument(0);
                    ReflectionTestUtils.setField(certification, "id", 101L);
                    return certification;
                });

        CertificationJudgementCommand command = service.createProcessing(
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                "certifications/42/image.jpg"
        );

        assertThat(command.certificationId()).isEqualTo(101L);
        ArgumentCaptor<Certification> certificationCaptor =
                ArgumentCaptor.forClass(Certification.class);
        verify(certificationRepository).saveAndFlush(certificationCaptor.capture());
        assertThat(certificationCaptor.getValue().getStatus())
                .isEqualTo(CertificationStatus.PROCESSING);
        assertThat(certificationCaptor.getValue().getRewardPoint()).isEqualTo(100);
    }

    /*
     * 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
     * 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
     * 기존 쿨다운 차단 테스트를 원형 보존한다.
     *
    @Test
    void blocksNewCertificationDuringRecentPassedCooldown() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(policyEvaluator.evaluate(USER_ID)).thenReturn(new CertificationPolicyResult(
                1,
                CertificationRestrictionType.COOLDOWN,
                LocalDateTime.of(2026, 7, 31, 14, 8),
                300,
                null,
                null
        ));

        assertThatThrownBy(() -> service.createProcessing(
                USER_ID,
                CertificationSource.GENERAL,
                null,
                "certifications/42/image.jpg"
        ))
                .isInstanceOf(CertificationException.class)
                .extracting("errorCode")
                .isEqualTo(CertificationErrorCode.COOLDOWN);

        verify(certificationRepository, never()).saveAndFlush(any());
    }
    */

    @Test
    void completesDuplicateGuideWithoutAiJudgement() {
        Certification certification = Certification.create(
                user,
                null,
                "certifications/42/image.jpg",
                CertificationSource.GENERAL
        );
        ReflectionTestUtils.setField(certification, "id", 102L);
        CertificationJudgementCommand command = new CertificationJudgementCommand(
                102L,
                USER_ID,
                CertificationSource.GENERAL,
                certification.getImageKey(),
                null
        );
        when(certificationRepository.findByIdAndUserIdForUpdate(102L, USER_ID))
                .thenReturn(Optional.of(certification));
        when(recycleGuideRepository.findById(GUIDE_ID)).thenReturn(Optional.of(guide));
        when(certificationRepository
                .existsByUserIdAndRecycleGuideIdAndStatusAndJudgedAtGreaterThanEqualAndJudgedAtLessThan(
                        org.mockito.ArgumentMatchers.eq(USER_ID),
                        org.mockito.ArgumentMatchers.eq(GUIDE_ID),
                        org.mockito.ArgumentMatchers.eq(CertificationStatus.PASSED),
                        any(),
                        any()
                )).thenReturn(true);

        CertificationJudgementPreparation preparation =
                service.prepareJudgement(command, GUIDE_ID);

        assertThat(preparation.isCompleted()).isTrue();
        assertThat(preparation.completedResponse().failureType())
                .isEqualTo(CertificationFailureType.DUPLICATE_GUIDE_TODAY);
        assertThat(preparation.completedResponse().earnedPoint()).isZero();
        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.FAILED);
        verify(templateProvider, never()).findActiveByRecycleGuideId(any());
        verify(aiJudgementRepository, never()).save(any());
        verify(pointService, never()).earnPoint(any(), any(), any(), any());
        verify(contributionService, never()).recordPassedCertification(any());
    }

    @Test
    void persistsAiJudgementAndReturnsVlmFailureResult() {
        Certification certification = Certification.create(
                user,
                guide,
                "certifications/42/image.jpg",
                CertificationSource.AFTER_SEARCH
        );
        ReflectionTestUtils.setField(certification, "id", 103L);
        when(certificationRepository.findByIdAndUserIdForUpdate(103L, USER_ID))
                .thenReturn(Optional.of(certification));
        CertificationJudgementContext context = new CertificationJudgementContext(
                103L,
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                "투명 페트병",
                "플라스틱",
                100,
                template()
        );
        CertificationVlmResult result = new CertificationVlmResult(
                AiJudgementResult.FAIL,
                "내용물이 남아 있습니다.",
                List.of("내용물을 비운 뒤 다시 촬영해 주세요."),
                "{\"result\":\"FAIL\"}"
        );

        CertificationCreateResponseDTO response =
                service.completeJudgement(context, result);

        assertThat(response.status()).isEqualTo(CertificationStatus.FAILED);
        assertThat(response.failureType())
                .isEqualTo(CertificationFailureType.VLM_JUDGEMENT_FAILED);
        assertThat(response.earnedPoint()).isZero();
        assertThat(response.retryAllowed()).isTrue();
        assertThat(certification.getJudgedAt()).isNotNull();

        ArgumentCaptor<AiJudgement> judgementCaptor =
                ArgumentCaptor.forClass(AiJudgement.class);
        verify(aiJudgementRepository).save(judgementCaptor.capture());
        assertThat(judgementCaptor.getValue().getJudgementTemplateId()).isEqualTo(7L);
        assertThat(judgementCaptor.getValue().getRetryGuide())
                .contains("내용물을 비운 뒤 다시 촬영해 주세요.");
        verify(pointService, never()).earnPoint(any(), any(), any(), any());
        verify(contributionService, never()).recordPassedCertification(any());
    }

    @Test
    void earnsPointWithStableIdempotencyKeyWhenJudgementPasses() {
        Certification certification = Certification.create(
                user,
                guide,
                "certifications/42/image.jpg",
                CertificationSource.AFTER_SEARCH
        );
        ReflectionTestUtils.setField(certification, "id", 104L);
        when(certificationRepository.findByIdAndUserIdForUpdate(104L, USER_ID))
                .thenReturn(Optional.of(certification));
        CertificationJudgementContext context = new CertificationJudgementContext(
                104L,
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                "투명 페트병",
                "플라스틱",
                100,
                template()
        );
        CertificationVlmResult result = new CertificationVlmResult(
                AiJudgementResult.PASS,
                "분리배출 기준을 충족합니다.",
                List.of(),
                "{\"result\":\"PASS\"}"
        );

        CertificationCreateResponseDTO response =
                service.completeJudgement(context, result);

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(response.earnedPoint()).isEqualTo(100);
        assertThat(response.retryAllowed()).isFalse();
        InOrder completionOrder = inOrder(pointService, contributionService);
        completionOrder.verify(pointService).earnPoint(
                USER_ID,
                104L,
                CertificationSource.AFTER_SEARCH,
                "certification:104:earn"
        );
        completionOrder.verify(contributionService).recordPassedCertification(104L);
    }

    @Test
    void propagatesPointFailureToRollbackCompletionTransaction() {
        Certification certification = Certification.create(
                user,
                guide,
                "certifications/42/image.jpg",
                CertificationSource.AFTER_SEARCH
        );
        ReflectionTestUtils.setField(certification, "id", 105L);
        when(certificationRepository.findByIdAndUserIdForUpdate(105L, USER_ID))
                .thenReturn(Optional.of(certification));
        CertificationJudgementContext context = new CertificationJudgementContext(
                105L,
                USER_ID,
                CertificationSource.AFTER_SEARCH,
                GUIDE_ID,
                "투명 페트병",
                "플라스틱",
                100,
                template()
        );
        CertificationVlmResult result = new CertificationVlmResult(
                AiJudgementResult.PASS,
                "분리배출 기준을 충족합니다.",
                List.of(),
                "{\"result\":\"PASS\"}"
        );
        // 데모데이 시현으로 비활성화된 DAILY_EARN_LIMIT_EXCEEDED 대신
        // 유지되는 포인트 상한 오류로 완료 트랜잭션 전파를 검증한다.
        PointException pointFailure = new PointException(PointErrorCode.POINT_LIMIT_EXCEEDED);
        doThrow(pointFailure).when(pointService).earnPoint(
                USER_ID,
                105L,
                CertificationSource.AFTER_SEARCH,
                "certification:105:earn"
        );

        assertThatThrownBy(() -> service.completeJudgement(context, result))
                .isSameAs(pointFailure);

        verify(aiJudgementRepository).save(any(AiJudgement.class));
        verify(pointService).earnPoint(
                USER_ID,
                105L,
                CertificationSource.AFTER_SEARCH,
                "certification:105:earn"
        );
        verify(contributionService, never()).recordPassedCertification(any());
    }

    private CertificationPolicyResult allowedPolicy() {
        return new CertificationPolicyResult(
                0,
                CertificationRestrictionType.NONE,
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
                "prompt",
                "{\"clean\":true}",
                "{\"contaminated\":true}",
                "세척 후 다시 촬영"
        );
    }
}
