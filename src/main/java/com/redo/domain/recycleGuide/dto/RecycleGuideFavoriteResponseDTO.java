package com.redo.domain.recycleGuide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

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
}
