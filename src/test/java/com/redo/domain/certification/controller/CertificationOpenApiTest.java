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
    void documentsDemoDayUnlimitedPolicy() throws Exception {
        mockMvc.perform(get("/v3/api-docs/03-certification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "일일 성공 횟수 제한과 성공 후 5분 대기를 적용하지"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "`dailyLimit=100`, `remainingCount>=1`"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "`policy.sameGuideDailyLimit=100`"
                )))
                .andExpect(jsonPath(
                        "$.components.schemas.CertificationHomeResponseDTO" +
                                ".properties.dailyLimit.type"
                ).value("integer"))
                .andExpect(jsonPath(
                        "$.components.schemas.CertificationHomeResponseDTO" +
                                ".properties.remainingCount.type"
                ).value("integer"))
                .andExpect(jsonPath(
                        "$.components.schemas.CertificationHomeResponseDTO.required"
                ).value(org.hamcrest.Matchers.hasItems(
                        "dailyLimit",
                        "remainingCount",
                        "policy"
                )))
                .andExpect(jsonPath(
                        "$.components.schemas.PolicyDTO.required"
                ).value(org.hamcrest.Matchers.hasItem("cooldownSeconds")))
                .andExpect(jsonPath(
                        "$.components.schemas.CertificationHomeResponseDTO" +
                                ".properties.dailyLimitEnabled"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.components.schemas.PolicyDTO.properties.cooldownEnabled"
                ).doesNotExist())
                .andExpect(jsonPath(
                        "$.components.schemas.RestrictionDTO" +
                                ".properties.retryAvailableAt.type"
                ).value(org.hamcrest.Matchers.containsInAnyOrder("string", "null")))
                .andExpect(jsonPath(
                        "$.components.schemas.RestrictionDTO" +
                                ".properties.processingCertificationId.type"
                ).value(org.hamcrest.Matchers.containsInAnyOrder("integer", "null")))
                .andExpect(jsonPath(
                        "$.components.schemas.RestrictionDTO" +
                                ".properties.statusPath.type"
                ).value(org.hamcrest.Matchers.containsInAnyOrder("string", "null")))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].get.description"
                ).value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "CERTIFICATION429_0"
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
                ).value(org.hamcrest.Matchers.containsString(
                        "일일 PASSED 횟수와 최근 PASSED 후 경과 시간은 신규 생성을 차단하지"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "오늘 PASSED된 동일 guide도 추가 인증할 수"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "DUPLICATE_GUIDE_TODAY"
                ))))
                .andExpect(jsonPath(
                        "$.paths['/api/certification'].post.description"
                ).value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "POINT_400_007"
                ))))
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
                ).value(org.hamcrest.Matchers.containsString(
                        "일일 PASSED 횟수와 최근 PASSED 후 경과 시간은 재촬영을 차단하지"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.containsString(
                        "오늘 PASSED된 동일 guide 여부로 차단하지"
                )))
                .andExpect(jsonPath(
                        "$.paths['/api/certification/{certificationId}/retry'].post.description"
                ).value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(
                        "DUPLICATE_GUIDE_TODAY"
                ))))
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
