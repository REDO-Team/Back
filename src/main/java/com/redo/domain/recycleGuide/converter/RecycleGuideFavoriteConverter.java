package com.redo.domain.recycleGuide.converter;

import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.entity.Mapping.RecycleGuideFavorite;

public class RecycleGuideFavoriteConverter {

    private RecycleGuideFavoriteConverter() {
    }

    public static RecycleGuideFavoriteResponseDTO.FavoriteResultDTO toFavoriteResultDTO(
            RecycleGuideFavorite favorite) {
        return RecycleGuideFavoriteResponseDTO.FavoriteResultDTO.builder()
                .favoriteId(favorite.getId())
                .guideId(favorite.getRecycleGuide().getId())
                .build();
    }
}
