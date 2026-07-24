package com.redo.domain.recycleGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
}
