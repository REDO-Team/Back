package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CertificationTest {

    private final User user = mock(User.class);
    private final RecycleGuide recycleGuide = mock(RecycleGuide.class);

    @Test
    void certificationSourcesDefineFixedRewardPoints() {
        assertThat(CertificationSource.GENERAL.rewardPoint()).isEqualTo(50);
        assertThat(CertificationSource.AFTER_SEARCH.rewardPoint()).isEqualTo(100);
    }

    @Test
    void createInitializesProcessingCertification() {
        Certification certification = createCertification();

        assertThat(certification.getUser()).isSameAs(user);
        assertThat(certification.getRecycleGuide()).isSameAs(recycleGuide);
        assertThat(certification.getImageKey()).isEqualTo("certification/original.jpg");
        assertThat(certification.getCertificationSource()).isEqualTo(CertificationSource.AFTER_SEARCH);
        assertThat(certification.getRewardPoint()).isEqualTo(100);
        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.PROCESSING);
        assertThat(certification.getAttemptCount()).isEqualTo(1);
        assertThat(certification.getJudgedAt()).isNull();
        assertThat(certification.getFailureType()).isNull();
    }

    @Test
    void createGeneralWaitsForClassificationBeforeAssigningGuide() {
        Certification certification = Certification.create(
                user,
                null,
                "certification/general.jpg",
                CertificationSource.GENERAL
        );

        assertThat(certification.getRecycleGuide()).isNull();
        assertThat(certification.getRewardPoint()).isEqualTo(50);

        certification.assignRecycleGuide(recycleGuide);

        assertThat(certification.getRecycleGuide()).isSameAs(recycleGuide);
    }

    @Test
    void createRejectsMissingAfterSearchGuide() {
        assertThatThrownBy(() -> Certification.create(
                user,
                null,
                "certification/after-search.jpg",
                CertificationSource.AFTER_SEARCH
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("recycleGuide must not be null for AFTER_SEARCH");
    }

    @Test
    void generalCertificationCannotCompleteBeforeGuideClassification() {
        Certification certification = Certification.create(
                user,
                null,
                "certification/general.jpg",
                CertificationSource.GENERAL
        );

        assertThatThrownBy(() -> certification.complete(AiJudgementResult.PASS, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recycle guide must be assigned before completion");
    }

    @Test
    void completeMapsPassToPassed() {
        Certification certification = createCertification();
        LocalDateTime judgedAt = LocalDateTime.of(2026, 7, 18, 10, 30);

        certification.complete(AiJudgementResult.PASS, judgedAt);

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.PASSED);
        assertThat(certification.getJudgedAt()).isEqualTo(judgedAt);
    }

    @Test
    void completeMapsFailToFailed() {
        Certification certification = createCertification();

        certification.complete(AiJudgementResult.FAIL, LocalDateTime.now());

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.FAILED);
        assertThat(certification.getFailureType()).isEqualTo(CertificationFailureType.VLM_JUDGEMENT_FAILED);
    }

    @Test
    void retryReplacesImageAndStartsNextAttempt() {
        Certification certification = createCertification();
        certification.complete(AiJudgementResult.FAIL, LocalDateTime.now());

        certification.retry("certification/retry.jpg");

        assertThat(certification.getImageKey()).isEqualTo("certification/retry.jpg");
        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.PROCESSING);
        assertThat(certification.getAttemptCount()).isEqualTo(2);
        assertThat(certification.getJudgedAt()).isNull();
        assertThat(certification.getFailureType()).isNull();
    }

    @Test
    void duplicateGuideFailureCannotBeRetried() {
        Certification certification = createCertification();
        certification.rejectDuplicateGuide(LocalDateTime.now());

        assertThat(certification.getStatus()).isEqualTo(CertificationStatus.FAILED);
        assertThat(certification.getFailureType()).isEqualTo(CertificationFailureType.DUPLICATE_GUIDE_TODAY);
        assertThatThrownBy(() -> certification.retry("certification/other-item.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only VLM judgement failures can be retried");
    }

    @Test
    void retryRejectsCertificationThatHasNotFailed() {
        Certification certification = createCertification();

        assertThatThrownBy(() -> certification.retry("certification/retry.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only VLM judgement failures can be retried");
    }

    @Test
    void completeRejectsAlreadyCompletedCertification() {
        Certification certification = createCertification();
        certification.complete(AiJudgementResult.PASS, LocalDateTime.now());

        assertThatThrownBy(() -> certification.complete(AiJudgementResult.FAIL, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only processing certifications can be completed");
    }

    private Certification createCertification() {
        return Certification.create(
                user,
                recycleGuide,
                "certification/original.jpg",
                CertificationSource.AFTER_SEARCH
        );
    }
}
