package com.redo.domain.certification.controller;

import com.redo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CertificationOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsPassedOnlyCooldownPolicy() throws Exception {
        mockMvc.perform(get("/v3/api-docs/03-certification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "최근 성공 인증(`PASSED`) 후 5분"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "`FAILED`는 신규 인증 쿨다운을 생성하거나"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "직전 완료 인증(`PASSED` 또는 `FAILED`)"
                ))));
    }

    @Test
    void documentsMultipartContractAndFrontendOutcomes() throws Exception {
        mockMvc.perform(get("/v3/api-docs/03-certification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/certification'].post").exists())
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.requestBody.content['multipart/form-data']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("CERTIFICATION201_1")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("정상 흐름에서는 polling하지 않습니다")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("평균 30초")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("CERTIFICATION503_1")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("PROCESSING_EXISTS")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("최근 PASSED 후 5분")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "FAILED는 신규 인증 쿨다운을 생성하거나 연장하지 않습니다"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("S3_413_001")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("point_transactions")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString("POINT_400_004")));
    }

    @Test
    void documentsRetryMultipartContractAndFrontendOutcomes() throws Exception {
        mockMvc.perform(get("/v3/api-docs/03-certification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry']" +
                                ".post.requestBody.content['multipart/form-data']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("CERTIFICATION200_10")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("CERTIFICATION200_11")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("5분 제한을 적용하지 않습니다")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("평균 30초")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("RETRY_NOT_ALLOWED")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("attemptCount")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString("GEMINI_504_001")));
    }
}
