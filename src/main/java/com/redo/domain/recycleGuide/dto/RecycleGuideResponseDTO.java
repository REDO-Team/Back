package com.redo.domain.recycleGuide.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class RecycleGuideResponseDTO {

    private RecycleGuideResponseDTO() {
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GuideDetailDTO {
        private final Long guideId;
        private final String name;
        private final String imageKey;
        private final String tip;
        private final String recycleCategory;
        private final List<String> guideSteps;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 검색 결과 응답 DTO (이미지 및 텍스트 공용)")
    public static class AiSearchResultDTO {
        @Schema(description = "식별 성공 여부", example = "true")
        private boolean isIdentified;

        @Schema(description = "해당 결과로 도출된 이유", example = "이미지 중앙에 투명 페트병이 명확하게 보입니다.")
        private String reason;

        @Schema(description = "식별된 배출 가이드 정보 (isIdentified가 false면 null)")
        private GuideDetailDTO guideDetail;
    }
}
