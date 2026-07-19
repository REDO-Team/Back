package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.repository.AiJudgementRepository;
import com.redo.domain.certification.repository.CertificationRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CertificationJpaMappingTest {

    @Test
    void certificationMapsRequiredTablesRelationshipsAndStringEnums() throws NoSuchFieldException {
        assertThat(Certification.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(Certification.class.getAnnotation(Table.class).name()).isEqualTo("certifications");
        assertJoinColumn(Certification.class, "user", "user_id", false);
        assertJoinColumn(Certification.class, "recycleGuide", "guide_id", false);
        assertColumn(Certification.class, "imageKey", "image_key", false);
        assertStringEnum(Certification.class, "status");
        assertStringEnum(Certification.class, "certificationSource");
    }

    @Test
    void aiJudgementMapsTemplateIdAndRawJsonWithoutTemplateRelationship() throws NoSuchFieldException {
        assertThat(AiJudgement.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(AiJudgement.class.getAnnotation(Table.class).name()).isEqualTo("ai_judgements");
        assertJoinColumn(AiJudgement.class, "certification", "certification_id", false);
        assertColumn(AiJudgement.class, "judgementTemplateId", "judgement_template_id", false);
        assertStringEnum(AiJudgement.class, "result");

        Field rawResponseJson = AiJudgement.class.getDeclaredField("rawResponseJson");
        assertThat(rawResponseJson.isAnnotationPresent(Lob.class)).isTrue();
        assertThat(rawResponseJson.getAnnotation(Column.class).columnDefinition()).isEqualTo("json");
    }

    @Test
    void enumsAndRepositoriesExposeOnlyTheSpecifiedPersistenceContract() {
        assertThat(CertificationStatus.values())
                .containsExactly(CertificationStatus.PROCESSING, CertificationStatus.PASSED, CertificationStatus.FAILED);
        assertThat(CertificationSource.values())
                .containsExactly(CertificationSource.GENERAL, CertificationSource.AFTER_SEARCH);
        assertThat(AiJudgementResult.values())
                .containsExactly(AiJudgementResult.PASS, AiJudgementResult.FAIL);
        assertThat(JpaRepository.class.isAssignableFrom(CertificationRepository.class)).isTrue();
        assertThat(JpaRepository.class.isAssignableFrom(AiJudgementRepository.class)).isTrue();
    }

    private void assertJoinColumn(
            Class<?> entityType,
            String fieldName,
            String columnName,
            boolean nullable
    ) throws NoSuchFieldException {
        JoinColumn joinColumn = entityType.getDeclaredField(fieldName).getAnnotation(JoinColumn.class);
        assertThat(joinColumn.name()).isEqualTo(columnName);
        assertThat(joinColumn.nullable()).isEqualTo(nullable);
    }

    private void assertColumn(
            Class<?> entityType,
            String fieldName,
            String columnName,
            boolean nullable
    ) throws NoSuchFieldException {
        Column column = entityType.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
    }

    private void assertStringEnum(Class<?> entityType, String fieldName) throws NoSuchFieldException {
        Enumerated enumerated = entityType.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }
}
