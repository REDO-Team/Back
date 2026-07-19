package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ai_judgements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiJudgement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Column(name = "judgement_template_id", nullable = false)
    private Long judgementTemplateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private AiJudgementResult result;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "retry_guide", length = 500)
    private String retryGuide;

    @Lob
    @Column(name = "raw_response_json", columnDefinition = "json")
    private String rawResponseJson;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AiJudgement create(
            Certification certification,
            Long judgementTemplateId,
            AiJudgementResult result,
            String reason,
            String retryGuide,
            String rawResponseJson
    ) {
        AiJudgement judgement = new AiJudgement();
        judgement.certification = Objects.requireNonNull(certification, "certification must not be null");
        judgement.judgementTemplateId = Objects.requireNonNull(
                judgementTemplateId,
                "judgementTemplateId must not be null"
        );
        judgement.result = Objects.requireNonNull(result, "result must not be null");
        judgement.reason = reason;
        judgement.retryGuide = retryGuide;
        judgement.rawResponseJson = rawResponseJson;
        return judgement;
    }
}
