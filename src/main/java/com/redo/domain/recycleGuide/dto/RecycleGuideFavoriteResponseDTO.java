package com.redo.domain.recycleGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class RecycleGuideFavoriteResponseDTO {

    private RecycleGuideFavoriteResponseDTO() {
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FavoriteResultDTO {
        private final Long favoriteId;
        private final Long guideId;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FavoriteGuideDTO {
        private final Long guideId;
        private final String name;
        private final String title;
        private final LocalDate favoritedAt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FavoriteGuideListDTO {
        private final List<FavoriteGuideDTO> favorites;
    }
}
