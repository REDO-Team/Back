package com.redo.domain.certification.controller;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.service.CertificationHomeService;
import com.redo.global.security.JwtAccessDeniedHandler;
import com.redo.global.security.JwtAuthenticationEntryPoint;
import com.redo.global.security.JwtAuthenticationFilter;
import com.redo.global.security.JwtUtil;
import com.redo.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private void stubAuthentication() {
        when(jwtUtil.validateToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(ACCESS_TOKEN)).thenReturn(USER_ID);
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
}
