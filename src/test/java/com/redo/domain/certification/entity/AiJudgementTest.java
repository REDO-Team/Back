package com.redo.domain.certification.entity;

import com.redo.domain.certification.enums.AiJudgementResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiJudgementTest {

    @Test
    void createStoresTemplateAndVlmEvidence() {
        Certification certification = mock(Certification.class);
        String rawResponseJson = "{\"result\":\"FAIL\"}";

        AiJudgement judgement = AiJudgement.create(
                certification,
                17L,
                AiJudgementResult.FAIL,
                "내용물이 남아 있습니다.",
                "내용물을 비우고 세척한 뒤 다시 촬영해 주세요.",
                rawResponseJson
        );

        assertThat(judgement.getCertification()).isSameAs(certification);
        assertThat(judgement.getJudgementTemplateId()).isEqualTo(17L);
        assertThat(judgement.getResult()).isEqualTo(AiJudgementResult.FAIL);
        assertThat(judgement.getReason()).isEqualTo("내용물이 남아 있습니다.");
        assertThat(judgement.getRetryGuide()).isEqualTo("내용물을 비우고 세척한 뒤 다시 촬영해 주세요.");
        assertThat(judgement.getRawResponseJson()).isEqualTo(rawResponseJson);
    }
}
