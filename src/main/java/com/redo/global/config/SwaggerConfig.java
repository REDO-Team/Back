package com.redo.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ReDO Backend API",
                description = "ReDO 백엔드 API 문서입니다.",
                version = "v1.0.0"
        ),
        security = { @SecurityRequirement(name = "bearerAuth") }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)

public class SwaggerConfig {

    @Value("${app.base-url}")
    private String baseUrl;


    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url(baseUrl)
                        .description("배포 서버"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("로컬 서버"));
    }


    /**
     * 전체 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("00-all")
                .displayName("00. 전체 API")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 회원, 인증 관련 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("01-user")
                .displayName("01. 회원 API")
                .pathsToMatch(
                        "/api/auth/**",
                        "/api/users/**",
                        "/api/user-profiles/**",
                        "/api/terms/**"
                )
                .build();
    }

    /**
     * AI 분리수거 정보/가이드 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi recycleGuideApi() {
        return GroupedOpenApi.builder()
                .group("02-recycle-guide")
                .displayName("02. 분리수거 가이드 API")
                .pathsToMatch(
                        "/api/recycle-categories/**",
                        "/api/recycle-guides/**",
                        "/api/recycle-guide-favorites/**"
                )
                .build();
    }

    /**
     * AI 배출 인증/판정 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi certificationApi() {
        return GroupedOpenApi.builder()
                .group("03-certification")
                .displayName("03. 배출 인증 API")
                .pathsToMatch(
                        "/api/certifications/**",
                        "/api/ai-judgements/**",
                        "/api/recycle-judgement-templates/**"
                )
                .build();
    }

    /**
     * 리워드, 포인트 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi rewardApi() {
        return GroupedOpenApi.builder()
                .group("04-reward")
                .displayName("04. 리워드/포인트 API")
                .pathsToMatch(
                        "/api/rewards/**",
                        "/api/shipping-addresses/**",
                        "/api/reward-products/**",
                        "/api/product-categories/**",
                        "/api/points/**",
                        "/api/point-transactions/**"
                )
                .build();
    }

    /**
     * 기여도 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi contributionApi() {
        return GroupedOpenApi.builder()
                .group("05-contribution")
                .displayName("05. 기여도 API")
                .pathsToMatch(
                        "/api/contributions/**",
                        "/api/contribution-events/**"
                )
                .build();
    }

    /**
     * 커뮤니티 API 그룹입니다.
     */
    @Bean
    public GroupedOpenApi communityApi() {
        return GroupedOpenApi.builder()
                .group("06-community")
                .displayName("06. 커뮤니티 API")
                .pathsToMatch(
                        "/api/community/**",
                        "/api/communities/**",
                        "/api/community-comments/**",
                        "/api/community-images/**"
                )
                .build();
    }
}
