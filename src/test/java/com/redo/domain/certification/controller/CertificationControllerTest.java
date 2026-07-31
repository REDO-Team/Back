package com.redo.domain.certification.controller;

import com.redo.domain.certification.dto.req.CertificationCreateRequestDTO;
import com.redo.domain.certification.dto.req.CertificationRetryRequestDTO;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;
import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.dto.res.CertificationRetryResponseDTO;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationStatus;
import com.redo.domain.certification.exception.CertificationException;
import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.domain.certification.service.CertificationCreateService;
import com.redo.domain.certification.service.CertificationHomeService;
import com.redo.domain.certification.service.CertificationRetryService;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import com.redo.global.security.JwtAccessDeniedHandler;
import com.redo.global.security.JwtAuthenticationEntryPoint;
import com.redo.global.security.JwtAuthenticationFilter;
import com.redo.global.security.JwtUtil;
import com.redo.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CertificationController.class,
        properties = "app.base-url=http://localhost:8080"
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class CertificationControllerTest {

    private static final Long USER_ID = 42L;
    private static final String ACCESS_TOKEN = "test-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificationHomeService certificationHomeService;

    @MockitoBean
    private CertificationCreateService certificationCreateService;

    @MockitoBean
    private CertificationRetryService certificationRetryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void returnsCertificationHomeResponseForAuthenticatedUser() throws Exception {
        stubAuthentication();
        when(certificationHomeService.getHome(USER_ID))
                .thenReturn(homeResponse(CertificationRestrictionType.NONE));

        mockMvc.perform(get("/api/certification")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("CERTIFICATION200_0"))
                .andExpect(jsonPath("$.message").value("인증하기 화면 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.dailyLimit").value(3))
                .andExpect(jsonPath("$.result.remainingCount").value(3))
                .andExpect(jsonPath("$.result.usedCount").value(0))
                .andExpect(jsonPath("$.result.canCertify").value(true))
                .andExpect(jsonPath("$.result.restriction.type").value("NONE"))
                .andExpect(jsonPath("$.result.policy.cooldownSeconds").value(300))
                .andExpect(jsonPath("$.result.rewardPolicy.generalCertificationPoint").value(50))
                .andExpect(jsonPath("$.result.rewardPolicy.afterSearchCertificationPoint").value(100))
                .andExpect(jsonPath("$.result.monthlyCertificationCount").doesNotExist());

        verify(certificationHomeService).getHome(USER_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "DAILY_LIMIT_EXCEEDED,CERTIFICATION200_1",
            "COOLDOWN,CERTIFICATION200_8",
            "PROCESSING_EXISTS,CERTIFICATION200_9"
    })
    void mapsRestrictionToSuccessCode(
            CertificationRestrictionType restrictionType,
            String expectedCode
    ) throws Exception {
        stubAuthentication();
        when(certificationHomeService.getHome(USER_ID))
                .thenReturn(homeResponse(restrictionType));

        mockMvc.perform(get("/api/certification")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.result.restriction.type")
                        .value(restrictionType.name()));
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/certification"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("AUTH_401_001"));

        verifyNoInteractions(certificationHomeService);
    }

    @Test
    void returnsPassedResultFromLongRunningMultipartRequest() throws Exception {
        stubAuthentication();
        when(certificationCreateService.create(
                eq(USER_ID),
                any(CertificationCreateRequestDTO.class)
        )).thenReturn(CompletableFuture.completedFuture(passedResponse()));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "can.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        MvcResult pending = mockMvc.perform(multipart("/api/certification")
                        .file(image)
                        .param("certificationSource", "GENERAL")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("CERTIFICATION201_0"))
                .andExpect(jsonPath("$.result.certificationId").value(101))
                .andExpect(jsonPath("$.result.status").value("PASSED"))
                .andExpect(jsonPath("$.result.earnedPoint").value(50))
                .andExpect(jsonPath("$.result.pollingIntervalSeconds").doesNotExist())
                .andExpect(jsonPath("$.result.statusPath").doesNotExist());

        assertGeneralCreateRequestWasBound();
    }

    @Test
    void returnsVlmFailureAsCompleted201Result() throws Exception {
        stubAuthentication();
        when(certificationCreateService.create(
                eq(USER_ID),
                any(CertificationCreateRequestDTO.class)
        )).thenReturn(CompletableFuture.completedFuture(failedResponse()));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "can.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        MvcResult pending = mockMvc.perform(multipart("/api/certification")
                        .file(image)
                        .param("certificationSource", "GENERAL")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CERTIFICATION201_1"))
                .andExpect(jsonPath("$.result.status").value("FAILED"))
                .andExpect(jsonPath("$.result.failureType")
                        .value("VLM_JUDGEMENT_FAILED"))
                .andExpect(jsonPath("$.result.earnedPoint").value(0))
                .andExpect(jsonPath("$.result.retryAllowed").value(true));

        assertGeneralCreateRequestWasBound();
    }

    @Test
    void returnsTypedProcessingConflictForFrontendRecovery() throws Exception {
        stubAuthentication();
        when(certificationCreateService.create(
                eq(USER_ID),
                any(CertificationCreateRequestDTO.class)
        )).thenThrow(new CertificationException(
                CertificationErrorCode.PROCESSING_EXISTS,
                CertificationErrorDetail.processing(
                        99L,
                        "/api/certification/99/status"
                )
        ));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "can.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        mockMvc.perform(multipart("/api/certification")
                        .file(image)
                        .param("certificationSource", "GENERAL")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("CERTIFICATION409_0"))
                .andExpect(jsonPath("$.errorDetail.type").value("PROCESSING_EXISTS"))
                .andExpect(jsonPath("$.errorDetail.certificationId").value(99))
                .andExpect(jsonPath("$.errorDetail.statusPath")
                        .value("/api/certification/99/status"));

        assertGeneralCreateRequestWasBound();
    }

    @Test
    void returnsGeminiTimeoutAsSystemErrorInsteadOfVlmFailure() throws Exception {
        stubAuthentication();
        CompletableFuture<CertificationCreateResponseDTO> failed =
                new CompletableFuture<>();
        failed.completeExceptionally(new GeminiException(GeminiErrorCode.TIMEOUT));
        when(certificationCreateService.create(
                eq(USER_ID),
                any(CertificationCreateRequestDTO.class)
        )).thenReturn(failed);
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "can.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        MvcResult pending = mockMvc.perform(multipart("/api/certification")
                        .file(image)
                        .param("certificationSource", "GENERAL")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("GEMINI_504_001"));

        assertGeneralCreateRequestWasBound();
    }

    @Test
    void returnsPassedResultFromLongRunningRetryRequest() throws Exception {
        stubAuthentication();
        when(certificationRetryService.retry(
                eq(USER_ID),
                eq(102L),
                any(CertificationRetryRequestDTO.class)
        )).thenReturn(CompletableFuture.completedFuture(retryPassedResponse()));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "retry.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        MvcResult pending = mockMvc.perform(multipart(
                        "/api/certification/{certificationId}/retry",
                        102L
                )
                        .file(image)
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("CERTIFICATION200_10"))
                .andExpect(jsonPath("$.result.certificationId").value(102))
                .andExpect(jsonPath("$.result.status").value("PASSED"))
                .andExpect(jsonPath("$.result.attemptCount").value(2))
                .andExpect(jsonPath("$.result.earnedPoint").value(100))
                .andExpect(jsonPath("$.result.pollingIntervalSeconds").doesNotExist())
                .andExpect(jsonPath("$.result.statusPath").doesNotExist());

        assertRetryRequestWasBound();
    }

    @Test
    void returnsRetryVlmFailureAsCompleted200Result() throws Exception {
        stubAuthentication();
        when(certificationRetryService.retry(
                eq(USER_ID),
                eq(102L),
                any(CertificationRetryRequestDTO.class)
        )).thenReturn(CompletableFuture.completedFuture(retryFailedResponse()));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "retry.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        MvcResult pending = mockMvc.perform(multipart(
                        "/api/certification/{certificationId}/retry",
                        102L
                )
                        .file(image)
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CERTIFICATION200_11"))
                .andExpect(jsonPath("$.result.status").value("FAILED"))
                .andExpect(jsonPath("$.result.failureType")
                        .value("VLM_JUDGEMENT_FAILED"))
                .andExpect(jsonPath("$.result.attemptCount").value(2))
                .andExpect(jsonPath("$.result.retryAllowed").value(true))
                .andExpect(jsonPath("$.result.retryPath")
                        .value("/api/certification/102/retry"));
    }

    @Test
    void returnsTypedConflictWhenPassedCertificationIsRetried() throws Exception {
        stubAuthentication();
        when(certificationRetryService.retry(
                eq(USER_ID),
                eq(102L),
                any(CertificationRetryRequestDTO.class)
        )).thenThrow(new CertificationException(
                CertificationErrorCode.PASSED_NOT_RETRYABLE,
                CertificationErrorDetail.type("PASSED_NOT_RETRYABLE")
        ));
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "retry.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        mockMvc.perform(multipart(
                        "/api/certification/{certificationId}/retry",
                        102L
                )
                        .file(image)
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("CERTIFICATION409_2"))
                .andExpect(jsonPath("$.errorDetail.type")
                        .value("PASSED_NOT_RETRYABLE"));
    }

    private void stubAuthentication() {
        when(jwtUtil.validateToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);
    }

    private void assertGeneralCreateRequestWasBound() {
        ArgumentCaptor<CertificationCreateRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(CertificationCreateRequestDTO.class);
        verify(certificationCreateService).create(eq(USER_ID), requestCaptor.capture());

        CertificationCreateRequestDTO request = requestCaptor.getValue();
        assertThat(request.certificationSource()).isEqualTo("GENERAL");
        assertThat(request.recycleGuideId()).isNull();
        assertThat(request.image()).isNotNull();
        assertThat(request.image().getOriginalFilename()).isEqualTo("can.jpg");
    }

    private void assertRetryRequestWasBound() {
        ArgumentCaptor<CertificationRetryRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(CertificationRetryRequestDTO.class);
        verify(certificationRetryService).retry(
                eq(USER_ID),
                eq(102L),
                requestCaptor.capture()
        );

        CertificationRetryRequestDTO request = requestCaptor.getValue();
        assertThat(request.image()).isNotNull();
        assertThat(request.image().getOriginalFilename()).isEqualTo("retry.jpg");
    }

    private CertificationHomeResponseDTO homeResponse(
            CertificationRestrictionType restrictionType
    ) {
        return new CertificationHomeResponseDTO(
                3,
                3,
                0,
                restrictionType == CertificationRestrictionType.NONE,
                new CertificationHomeResponseDTO.RestrictionDTO(
                        restrictionType,
                        null,
                        0,
                        null,
                        null
                ),
                new CertificationHomeResponseDTO.PolicyDTO(300, 1, true),
                new CertificationHomeResponseDTO.RewardPolicyDTO(50, 100)
        );
    }

    private CertificationCreateResponseDTO passedResponse() {
        return new CertificationCreateResponseDTO(
                101L,
                CertificationStatus.PASSED,
                null,
                12L,
                "캔",
                "금속",
                50,
                null,
                List.of(),
                false,
                null,
                LocalDateTime.of(2026, 7, 31, 14, 3)
        );
    }

    private CertificationCreateResponseDTO failedResponse() {
        return new CertificationCreateResponseDTO(
                102L,
                CertificationStatus.FAILED,
                CertificationFailureType.VLM_JUDGEMENT_FAILED,
                12L,
                "캔",
                "금속",
                0,
                "내용물이 남아 있습니다.",
                List.of("내용물을 비운 뒤 다시 촬영해 주세요."),
                true,
                "/api/certification/102/retry",
                LocalDateTime.of(2026, 7, 31, 14, 3)
        );
    }

    private CertificationRetryResponseDTO retryPassedResponse() {
        return new CertificationRetryResponseDTO(
                102L,
                CertificationStatus.PASSED,
                null,
                2,
                12L,
                "투명 페트병",
                "플라스틱",
                100,
                null,
                List.of(),
                false,
                null,
                LocalDateTime.of(2026, 7, 31, 14, 8)
        );
    }

    private CertificationRetryResponseDTO retryFailedResponse() {
        return new CertificationRetryResponseDTO(
                102L,
                CertificationStatus.FAILED,
                CertificationFailureType.VLM_JUDGEMENT_FAILED,
                2,
                12L,
                "투명 페트병",
                "플라스틱",
                0,
                "이물질이 남아 있습니다.",
                List.of("이물질을 제거한 뒤 다시 촬영해 주세요."),
                true,
                "/api/certification/102/retry",
                LocalDateTime.of(2026, 7, 31, 14, 8)
        );
    }
}
