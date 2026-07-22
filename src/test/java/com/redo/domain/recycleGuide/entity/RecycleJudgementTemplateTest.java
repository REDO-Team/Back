package com.redo.domain.recycleGuide.entity;

import com.redo.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class RecycleJudgementTemplateTest {

    private final RecycleGuide recycleGuide = mock(RecycleGuide.class);

    @Test
    void createStoresGuideAndJudgementTemplateData() {
        RecycleJudgementTemplate template = createTemplate("다시 세척한 뒤 촬영해 주세요.");

        assertThat(template.getRecycleGuide()).isSameAs(recycleGuide);
        assertThat(template.getVersion()).isEqualTo(1);
        assertThat(template.getPassConditionsJson()).isEqualTo("[\"내용물이 비어 있음\"]");
        assertThat(template.getFailConditionsJson()).isEqualTo("[\"오염물이 남아 있음\"]");
        assertThat(template.getPromptTemplate()).isEqualTo("저장된 기준으로 인증 사진을 판정하세요.");
        assertThat(template.getRetryGuideTemplate()).isEqualTo("다시 세척한 뒤 촬영해 주세요.");
        assertThat(template.getIsActive()).isTrue();
    }

    @Test
    void createAllowsNullRetryGuideTemplate() {
        RecycleJudgementTemplate template = createTemplate(null);

        assertThat(template.getRetryGuideTemplate()).isNull();
    }

    @Test
    void createRejectsMissingGuide() {
        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                null,
                1,
                "[\"PASS\"]",
                "[\"FAIL\"]",
                "판정 프롬프트",
                null,
                true
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("recycleGuide must not be null");
    }

    @Test
    void createRejectsNonPositiveVersion() {
        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                0,
                "[\"PASS\"]",
                "[\"FAIL\"]",
                "판정 프롬프트",
                null,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be positive");
    }

    @Test
    void createRejectsMissingVersionAndActiveState() {
        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                null,
                "[\"PASS\"]",
                "[\"FAIL\"]",
                "판정 프롬프트",
                null,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("version must be positive");

        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                "[\"PASS\"]",
                "[\"FAIL\"]",
                "판정 프롬프트",
                null,
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("isActive must not be null");
    }

    @Test
    void createRejectsBlankRequiredTemplateText() {
        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                " ",
                "[\"FAIL\"]",
                "판정 프롬프트",
                null,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passConditionsJson must not be blank");

        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                "[\"PASS\"]",
                " ",
                "판정 프롬프트",
                null,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("failConditionsJson must not be blank");

        assertThatThrownBy(() -> RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                "[\"PASS\"]",
                "[\"FAIL\"]",
                " ",
                null,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("promptTemplate must not be blank");
    }

    @Test
    void mapsErdPersistenceContractWithoutRewardPoint() throws NoSuchFieldException {
        assertThat(RecycleJudgementTemplate.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(RecycleJudgementTemplate.class.getAnnotation(Table.class).name())
                .isEqualTo("recycle_judgement_templates");
        assertThat(BaseEntity.class.isAssignableFrom(RecycleJudgementTemplate.class)).isTrue();

        Field recycleGuideField = RecycleJudgementTemplate.class.getDeclaredField("recycleGuide");
        ManyToOne manyToOne = recycleGuideField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = recycleGuideField.getAnnotation(JoinColumn.class);
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("guide_id");
        assertThat(joinColumn.nullable()).isFalse();

        assertColumn("version", "version", false, null, false);
        assertColumn("passConditionsJson", "pass_conditions_json", false, "json", false);
        assertColumn("failConditionsJson", "fail_conditions_json", false, "json", false);
        assertColumn("promptTemplate", "prompt_template", false, "TEXT", true);
        assertColumn("retryGuideTemplate", "retry_guide_template", true, "TEXT", true);
        assertColumn("isActive", "is_active", false, null, false);

        assertThat(RecycleJudgementTemplate.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("rewardPoint");
    }

    private RecycleJudgementTemplate createTemplate(String retryGuideTemplate) {
        return RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                "[\"내용물이 비어 있음\"]",
                "[\"오염물이 남아 있음\"]",
                "저장된 기준으로 인증 사진을 판정하세요.",
                retryGuideTemplate,
                true
        );
    }

    private void assertColumn(
            String fieldName,
            String columnName,
            boolean nullable,
            String columnDefinition,
            boolean lob
    ) throws NoSuchFieldException {
        Field field = RecycleJudgementTemplate.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(field.isAnnotationPresent(Lob.class)).isEqualTo(lob);
        if (columnDefinition != null) {
            assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
        }
    }
}
