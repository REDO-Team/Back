package com.redo.domain.certification.service;

import com.redo.TestcontainersConfiguration;
import com.redo.domain.certification.dto.CertificationJudgementContext;
import com.redo.domain.certification.dto.CertificationVlmResult;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.entity.AiJudgement;
import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationJudgementMode;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.AiJudgementRepository;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.contribution.entity.ContributionEvent;
import com.redo.domain.contribution.exception.ContributionException;
import com.redo.domain.contribution.exception.code.ContributionErrorCode;
import com.redo.domain.contribution.repository.ContributionEventRepository;
import com.redo.domain.contribution.service.ContributionService;
import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.point.repository.PointTransactionRepository;
import com.redo.domain.recycleGuide.dto.ActiveRecycleJudgementTemplate;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CertificationPointContributionIntegrationTest {

    private static final String ITEM_NAME = "투명 페트병";
    private static final String CATEGORY_NAME = "플라스틱";
    private static final String IMAGE_KEY = "certifications/integration/original.jpg";
    private static final String RETRY_IMAGE_KEY = "certifications/integration/retry.jpg";

    @Autowired
    private CertificationTransactionService transactionService;
    @Autowired
    private CertificationRepository certificationRepository;
    @Autowired
    private AiJudgementRepository aiJudgementRepository;
    @Autowired
    private PointTransactionRepository pointTransactionRepository;
    @Autowired
    private ContributionEventRepository contributionEventRepository;
    @Autowired
    private RecycleGuideRepository recycleGuideRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private ContributionService contributionService;

    private User user;
    private RecycleGuide guide;

    @BeforeEach
    void setUp() {
        contributionEventRepository.deleteAllInBatch();
        pointTransactionRepository.deleteAllInBatch();
        aiJudgementRepository.deleteAllInBatch();
        certificationRepository.deleteAllInBatch();
        recycleGuideRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        user = userRepository.saveAndFlush(User.createGeneral(
                "certification-integration-user",
                "certification-integration@example.com",
                "password-hash"
        ));
        guide = BeanUtils.instantiateClass(RecycleGuide.class);
        ReflectionTestUtils.setField(guide, "name", ITEM_NAME);
        guide = recycleGuideRepository.saveAndFlush(guide);
        entityManager.clear();
    }

    @Test
    void contributionFailureRollsBackCertificationPointAndContributionTogether() {
        Certification certification = saveProcessingCertification(IMAGE_KEY);
        AtomicBoolean contributionWasRecorded = new AtomicBoolean(false);
        ContributionException contributionFailure = new ContributionException(
                ContributionErrorCode.DUPLICATE_CONTRIBUTION_EVENT
        );
        doAnswer(invocation -> {
            invocation.callRealMethod();
            contributionWasRecorded.set(true);
            throw contributionFailure;
        }).when(contributionService).recordPassedCertification(certification.getId());

        assertThatThrownBy(() -> transactionService.completeJudgement(
                createContext(certification, CertificationJudgementMode.CREATE_AFTER_SEARCH),
                passResult()
        )).isSameAs(contributionFailure);

        entityManager.clear();
        Certification rolledBackCertification = certificationRepository
                .findById(certification.getId())
                .orElseThrow();

        assertThat(contributionWasRecorded).isTrue();
        assertThat(rolledBackCertification.getStatus())
                .isEqualTo(CertificationStatus.PROCESSING);
        assertThat(rolledBackCertification.getJudgedAt()).isNull();
        assertThat(aiJudgementRepository.count()).isZero();
        assertThat(pointTransactionRepository.count()).isZero();
        assertThat(contributionEventRepository.count()).isZero();
        assertThat(userRepository.findById(user.getId()).orElseThrow().getTotalPoints())
                .isZero();
    }

    @Test
    void retrySuccessCommitsCertificationPointAndContributionTogether() {
        Certification certification = Certification.create(
                user,
                guide,
                IMAGE_KEY,
                CertificationSource.AFTER_SEARCH
        );
        certification.complete(
                AiJudgementResult.FAIL,
                LocalDateTime.now().minusMinutes(10)
        );
        certification.retry(RETRY_IMAGE_KEY);
        certification = certificationRepository.saveAndFlush(certification);
        entityManager.clear();

        CertificationCreateResponseDTO response = transactionService.completeJudgement(
                createContext(certification, CertificationJudgementMode.RETRY),
                passResult()
        );

        entityManager.clear();
        Certification committedCertification = certificationRepository
                .findById(certification.getId())
                .orElseThrow();
        AiJudgement judgement = aiJudgementRepository
                .findTopByCertificationIdOrderByCreatedAtDesc(certification.getId())
                .orElseThrow();
        PointTransaction pointTransaction = pointTransactionRepository.findAll()
                .get(0);
        ContributionEvent contributionEvent = contributionEventRepository
                .findByCertificationId(certification.getId())
                .orElseThrow();

        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(committedCertification.getStatus())
                .isEqualTo(CertificationStatus.PASSED);
        assertThat(committedCertification.getAttemptCount()).isEqualTo(2);
        assertThat(aiJudgementRepository.count()).isEqualTo(1);
        assertThat(judgement.getResult()).isEqualTo(AiJudgementResult.PASS);
        assertThat(pointTransactionRepository.count()).isEqualTo(1);
        assertThat(pointTransaction.getTransactionType())
                .isEqualTo(PointTransactionType.EARN);
        assertThat(pointTransaction.getAmount()).isEqualTo(100);
        assertThat(pointTransaction.getIdempotencyKey())
                .isEqualTo("certification:" + certification.getId() + ":earn");
        assertThat(pointTransaction.getCertification().getId())
                .isEqualTo(certification.getId());
        assertThat(contributionEventRepository.count()).isEqualTo(1);
        assertThat(contributionEvent.getCertification().getId())
                .isEqualTo(certification.getId());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getTotalPoints())
                .isEqualTo(100);
    }

    @Test
    void fourthDailyPassStillCommitsCertificationPointAndContribution() {
        for (int index = 1; index <= 3; index++) {
            Certification previous = Certification.create(
                    user,
                    guide,
                    "certifications/integration/previous-" + index + ".jpg",
                    CertificationSource.AFTER_SEARCH
            );
            previous.complete(
                    AiJudgementResult.PASS,
                    LocalDateTime.now().minusMinutes(10 + index)
            );
            previous = certificationRepository.saveAndFlush(previous);
            pointTransactionRepository.saveAndFlush(PointTransaction.builder()
                    .user(user)
                    .certification(previous)
                    .transactionType(PointTransactionType.EARN)
                    .amount(100)
                    .idempotencyKey("certification:" + previous.getId() + ":earn")
                    .build());
        }
        user.addPoint(300);
        userRepository.saveAndFlush(user);

        Certification fourth = saveProcessingCertification(IMAGE_KEY);
        entityManager.clear();

        CertificationCreateResponseDTO response = transactionService.completeJudgement(
                createContext(fourth, CertificationJudgementMode.CREATE_AFTER_SEARCH),
                passResult()
        );

        entityManager.clear();
        assertThat(response.status()).isEqualTo(CertificationStatus.PASSED);
        assertThat(pointTransactionRepository.count()).isEqualTo(4);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getTotalPoints())
                .isEqualTo(400);
        assertThat(contributionEventRepository.findByCertificationId(fourth.getId()))
                .isPresent();
    }

    private Certification saveProcessingCertification(String imageKey) {
        return certificationRepository.saveAndFlush(Certification.create(
                user,
                guide,
                imageKey,
                CertificationSource.AFTER_SEARCH
        ));
    }

    private CertificationJudgementContext createContext(
            Certification certification,
            CertificationJudgementMode mode
    ) {
        return new CertificationJudgementContext(
                certification.getId(),
                user.getId(),
                CertificationSource.AFTER_SEARCH,
                guide.getId(),
                ITEM_NAME,
                CATEGORY_NAME,
                CertificationSource.AFTER_SEARCH.rewardPoint(),
                new ActiveRecycleJudgementTemplate(
                        1L,
                        guide.getId(),
                        1,
                        "prompt",
                        "{\"clean\":true}",
                        "{\"contaminated\":true}",
                        "세척 후 다시 촬영"
                ),
                mode
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
}
