package com.redo.domain.recycleGuide.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RecycleGuideRequestDTO {

    @Getter
    @NoArgsConstructor
    @Schema(description = "텍스트 기반 문제 상황 검색 요청 DTO")
    public static class TextSearchDTO {

        @NotBlank(message = "문제 상황 텍스트를 입력해 주세요.")
        @Schema(description = "사용자가 입력한 문제 상황 텍스트", example = "깨진 유리컵은 어떻게 버리나요?")
        private String query;

        public TextSearchDTO(String query) {
            this.query = query;
        }
    }
}
