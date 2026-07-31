package com.redo.domain.recycleGuide.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.TestcontainersConfiguration;
import com.redo.domain.recycleGuide.repository.RecycleJudgementTemplateRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RecycleJudgementTemplatePersistenceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RecycleJudgementTemplateRepository templateRepository;

    @Test
    void persistsAndReadsJsonConditionsFromMySqlJsonColumns() throws Exception {
        RecycleGuide recycleGuide = new RecycleGuide();
        ReflectionTestUtils.setField(recycleGuide, "name", "투명 페트병");
        entityManager.persist(recycleGuide);

        String passConditionsJson = """
                [{"code":"EMPTY","description":"내용물이 비어 있음"}]
                """.trim();
        String failConditionsJson = """
                [{"code":"CONTAMINATED","description":"오염물이 남아 있음"}]
                """.trim();
        RecycleJudgementTemplate template = RecycleJudgementTemplate.create(
                recycleGuide,
                1,
                passConditionsJson,
                failConditionsJson,
                "저장된 기준으로 인증 사진을 판정하세요.",
                null,
                true
        );
        entityManager.persist(template);
        entityManager.flush();

        Long templateId = template.getId();
        entityManager.clear();

        RecycleJudgementTemplate found = entityManager.find(RecycleJudgementTemplate.class, templateId);
        JsonNode expectedPassConditions = objectMapper.readTree(passConditionsJson);
        JsonNode expectedFailConditions = objectMapper.readTree(failConditionsJson);

        assertThat(objectMapper.readTree(found.getPassConditionsJson())).isEqualTo(expectedPassConditions);
        assertThat(objectMapper.readTree(found.getFailConditionsJson())).isEqualTo(expectedFailConditions);
        assertThat(found.getPromptTemplate()).isEqualTo("저장된 기준으로 인증 사진을 판정하세요.");
        assertThat(found.getRetryGuideTemplate()).isNull();
        assertThat(found.getIsActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();

        Object extractedCondition = entityManager.createNativeQuery("""
                        SELECT JSON_UNQUOTE(JSON_EXTRACT(pass_conditions_json, '$[0].description'))
                        FROM recycle_judgement_templates
                        WHERE id = :templateId
                        """)
                .setParameter("templateId", templateId)
                .getSingleResult();
        assertThat(extractedCondition).isEqualTo("내용물이 비어 있음");
    }

    @Test
    void findsHighestActiveVersionForGuide() {
        RecycleGuide recycleGuide = new RecycleGuide();
        ReflectionTestUtils.setField(recycleGuide, "name", "알루미늄 캔");
        entityManager.persist(recycleGuide);

        entityManager.persist(template(recycleGuide, 1, true));
        RecycleJudgementTemplate expected = template(recycleGuide, 2, true);
        entityManager.persist(expected);
        entityManager.persist(template(recycleGuide, 3, false));
        entityManager.flush();

        RecycleJudgementTemplate found = templateRepository
                .findTopByRecycleGuideIdAndIsActiveTrueOrderByVersionDesc(
                        recycleGuide.getId()
                )
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(expected.getId());
        assertThat(found.getVersion()).isEqualTo(2);
    }

    private RecycleJudgementTemplate template(
            RecycleGuide recycleGuide,
            int version,
            boolean active
    ) {
        return RecycleJudgementTemplate.create(
                recycleGuide,
                version,
                "[{\"code\":\"EMPTY\"}]",
                "[{\"code\":\"CONTAMINATED\"}]",
                "판정 프롬프트 " + version,
                "다시 촬영해 주세요.",
                active
        );
    }
}
