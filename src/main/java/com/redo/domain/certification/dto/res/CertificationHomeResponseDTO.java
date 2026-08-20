package com.redo.domain.certification.dto.res;

import com.redo.domain.certification.enums.CertificationRestrictionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CertificationHomeResponseDTO(
        @Schema(
                description = "프론트 무수정 호환을 위한 데모데이 한도 표현값",
                format = "int32",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int dailyLimit,

        @Schema(
                description = "프론트 무수정 호환을 위한 남은 횟수 표현값. 최소 1",
                format = "int32",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int remainingCount,

        @Schema(
                description = "Asia/Seoul 기준 오늘 PASSED 인증 수",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long usedCount,

        @Schema(description = "신규 인증 가능 여부", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean canCertify,

        @Schema(description = "현재 신규 인증 제한 상태", requiredMode = Schema.RequiredMode.REQUIRED)
        RestrictionDTO restriction,

        @Schema(description = "인증 정책", requiredMode = Schema.RequiredMode.REQUIRED)
        PolicyDTO policy,

        @Schema(description = "인증 방식별 리워드 정책", requiredMode = Schema.RequiredMode.REQUIRED)
        RewardPolicyDTO rewardPolicy
) {

    public record RestrictionDTO(
            @Schema(description = "제한 유형", requiredMode = Schema.RequiredMode.REQUIRED)
            CertificationRestrictionType type,

            @Schema(
                    description = "재시도 가능 시각. 데모데이 정책에서는 항상 null",
                    types = {"string", "null"},
                    format = "date-time",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            LocalDateTime retryAvailableAt,

            @Schema(
                    description = "재시도까지 남은 초. 데모데이 정책에서는 항상 0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            long remainingSeconds,

            @Schema(
                    description = "진행 중 인증 ID. PROCESSING_EXISTS가 아니면 null",
                    types = {"integer", "null"},
                    format = "int64",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            Long processingCertificationId,

            @Schema(
                    description = "진행 중 인증 상태 조회 경로. PROCESSING_EXISTS가 아니면 null",
                    types = {"string", "null"},
                    nullable = true,
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String statusPath
    ) {
    }

    public record PolicyDTO(
            @Schema(
                    description = "인증 성공 후 대기 시간(초). 데모데이 정책에서는 0",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            long cooldownSeconds,

            @Schema(
                    description = "동일 가이드의 일일 PASSED 허용 횟수",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            int sameGuideDailyLimit,

            @Schema(description = "실시간 촬영 이미지 요구 여부", requiredMode = Schema.RequiredMode.REQUIRED)
            boolean liveCaptureOnly
    ) {
    }

    public record RewardPolicyDTO(
            @Schema(description = "일반 인증 성공 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
            int generalCertificationPoint,

            @Schema(description = "검색 후 인증 성공 포인트", requiredMode = Schema.RequiredMode.REQUIRED)
            int afterSearchCertificationPoint
    ) {
    }
}
