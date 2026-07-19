package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
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

    public static Certification create(
            User user,
            RecycleGuide recycleGuide,
            String imageKey,
            CertificationSource certificationSource,
            Integer rewardPoint
    ) {
        Certification certification = new Certification();
        certification.user = Objects.requireNonNull(user, "user must not be null");
        certification.recycleGuide = Objects.requireNonNull(recycleGuide, "recycleGuide must not be null");
        certification.imageKey = requireText(imageKey, "imageKey");
        certification.certificationSource = Objects.requireNonNull(
                certificationSource,
                "certificationSource must not be null"
        );
        certification.rewardPoint = requireNonNegative(rewardPoint, "rewardPoint");
        certification.status = CertificationStatus.PROCESSING;
        certification.attemptCount = 1;
        return certification;
    }

    public void complete(AiJudgementResult result, LocalDateTime judgedAt) {
        if (status != CertificationStatus.PROCESSING) {
            throw new IllegalStateException("Only processing certifications can be completed");
        }

        this.status = switch (Objects.requireNonNull(result, "result must not be null")) {
            case PASS -> CertificationStatus.PASSED;
            case FAIL -> CertificationStatus.FAILED;
        };
        this.judgedAt = Objects.requireNonNull(judgedAt, "judgedAt must not be null");
    }

    public void retry(String imageKey) {
        if (status != CertificationStatus.FAILED) {
            throw new IllegalStateException("Only failed certifications can be retried");
        }

        this.imageKey = requireText(imageKey, "imageKey");
        this.status = CertificationStatus.PROCESSING;
        this.attemptCount += 1;
        this.judgedAt = null;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static Integer requireNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }
}
