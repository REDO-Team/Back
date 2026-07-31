package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.user.entity.User;
import com.redo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "certifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Certification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id")
    private RecycleGuide recycleGuide;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CertificationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "certification_source", nullable = false, length = 30)
    private CertificationSource certificationSource;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "judged_at")
    private LocalDateTime judgedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", length = 50)
    private CertificationFailureType failureType;

    public static Certification create(
            User user,
            RecycleGuide recycleGuide,
            String imageKey,
            CertificationSource certificationSource
    ) {
        CertificationSource requiredSource = Objects.requireNonNull(
                certificationSource,
                "certificationSource must not be null"
        );
        Certification certification = new Certification();
        certification.user = Objects.requireNonNull(user, "user must not be null");
        certification.recycleGuide = validateRecycleGuide(requiredSource, recycleGuide);
        certification.imageKey = requireText(imageKey, "imageKey");
        certification.certificationSource = requiredSource;
        certification.rewardPoint = requiredSource.rewardPoint();
        certification.status = CertificationStatus.PROCESSING;
        certification.attemptCount = 1;
        return certification;
    }

    public void assignRecycleGuide(RecycleGuide recycleGuide) {
        if (certificationSource != CertificationSource.GENERAL) {
            throw new IllegalStateException("Only general certifications can assign a classified recycle guide");
        }
        if (status != CertificationStatus.PROCESSING) {
            throw new IllegalStateException("Recycle guide can only be assigned while processing");
        }
        if (this.recycleGuide != null) {
            throw new IllegalStateException("Recycle guide has already been assigned");
        }

        this.recycleGuide = Objects.requireNonNull(recycleGuide, "recycleGuide must not be null");
    }

    public void complete(AiJudgementResult result, LocalDateTime judgedAt) {
        if (status != CertificationStatus.PROCESSING) {
            throw new IllegalStateException("Only processing certifications can be completed");
        }
        if (recycleGuide == null) {
            throw new IllegalStateException("Recycle guide must be assigned before completion");
        }

        AiJudgementResult requiredResult = Objects.requireNonNull(result, "result must not be null");
        this.status = requiredResult == AiJudgementResult.PASS
                ? CertificationStatus.PASSED
                : CertificationStatus.FAILED;
        this.failureType = requiredResult == AiJudgementResult.FAIL
                ? CertificationFailureType.VLM_JUDGEMENT_FAILED
                : null;
        this.judgedAt = Objects.requireNonNull(judgedAt, "judgedAt must not be null");
    }

    public void rejectDuplicateGuide(LocalDateTime judgedAt) {
        if (status != CertificationStatus.PROCESSING) {
            throw new IllegalStateException("Only processing certifications can be rejected");
        }
        if (recycleGuide == null) {
            throw new IllegalStateException("Recycle guide must be assigned before duplicate rejection");
        }

        this.status = CertificationStatus.FAILED;
        this.failureType = CertificationFailureType.DUPLICATE_GUIDE_TODAY;
        this.judgedAt = Objects.requireNonNull(judgedAt, "judgedAt must not be null");
    }

    public void retry(String imageKey) {
        if (status != CertificationStatus.FAILED
                || failureType != CertificationFailureType.VLM_JUDGEMENT_FAILED) {
            throw new IllegalStateException("Only VLM judgement failures can be retried");
        }

        this.imageKey = requireText(imageKey, "imageKey");
        this.status = CertificationStatus.PROCESSING;
        this.attemptCount += 1;
        this.judgedAt = null;
        this.failureType = null;
    }

    public void restoreRetry(
            String previousImageKey,
            LocalDateTime previousJudgedAt,
            int previousAttemptCount
    ) {
        if (status != CertificationStatus.PROCESSING
                || attemptCount != previousAttemptCount + 1) {
            throw new IllegalStateException("Only the current retry attempt can be restored");
        }

        this.imageKey = requireText(previousImageKey, "previousImageKey");
        this.status = CertificationStatus.FAILED;
        this.attemptCount = previousAttemptCount;
        this.judgedAt = Objects.requireNonNull(
                previousJudgedAt,
                "previousJudgedAt must not be null"
        );
        this.failureType = CertificationFailureType.VLM_JUDGEMENT_FAILED;
    }

    private static RecycleGuide validateRecycleGuide(
            CertificationSource certificationSource,
            RecycleGuide recycleGuide
    ) {
        CertificationSource requiredSource = Objects.requireNonNull(
                certificationSource,
                "certificationSource must not be null"
        );

        if (requiredSource == CertificationSource.AFTER_SEARCH) {
            return Objects.requireNonNull(recycleGuide, "recycleGuide must not be null for AFTER_SEARCH");
        }
        if (recycleGuide != null) {
            throw new IllegalArgumentException("recycleGuide must be null before GENERAL classification");
        }
        return null;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

}
