package com.redo.domain.recycleGuide.entity;

import com.redo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.Objects;

@Entity
@Table(name = "recycle_judgement_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleJudgementTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guide_id", nullable = false)
    private RecycleGuide recycleGuide;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "pass_conditions_json", nullable = false, columnDefinition = "json")
    private String passConditionsJson;

    @Column(name = "fail_conditions_json", nullable = false, columnDefinition = "json")
    private String failConditionsJson;

    @Lob
    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;

    @Lob
    @Column(name = "retry_guide_template", columnDefinition = "TEXT")
    private String retryGuideTemplate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    public static RecycleJudgementTemplate create(
            RecycleGuide recycleGuide,
            Integer version,
            String passConditionsJson,
            String failConditionsJson,
            String promptTemplate,
            String retryGuideTemplate,
            Boolean isActive
    ) {
        RecycleJudgementTemplate template = new RecycleJudgementTemplate();
        template.recycleGuide = Objects.requireNonNull(recycleGuide, "recycleGuide must not be null");
        template.version = requirePositive(version, "version");
        template.passConditionsJson = requireText(passConditionsJson, "passConditionsJson");
        template.failConditionsJson = requireText(failConditionsJson, "failConditionsJson");
        template.promptTemplate = requireText(promptTemplate, "promptTemplate");
        template.retryGuideTemplate = retryGuideTemplate;
        template.isActive = Objects.requireNonNull(isActive, "isActive must not be null");
        return template;
    }

    private static Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value < 1) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
