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
    public static class CategoryWithGuidesDTO {
        private final Long categoryId;
        private final String categoryName;
        private final List<GuideSimpleDTO> guides;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GuideSimpleDTO {
        private final Long guideId;
        private final String guideName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GuideDetailDTO {
        private final String name;
        private final String imageUrl;
        private final String tip;
        private final String recycleCategory;
        private final List<String> guideSteps;
    }
}
