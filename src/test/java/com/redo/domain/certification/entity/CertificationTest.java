package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
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
    }

    @Test
    void retryRejectsCertificationThatHasNotFailed() {
        Certification certification = createCertification();

        assertThatThrownBy(() -> certification.retry("certification/retry.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only failed certifications can be retried");
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
                CertificationSource.AFTER_SEARCH,
                100
        );
    }
}
