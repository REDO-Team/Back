package com.redo.domain.recycleGuide.converter;

import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.entity.Mapping.RecycleGuideFavorite;

import java.util.List;

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

    public static RecycleGuideFavoriteResponseDTO.FavoriteGuideDTO toFavoriteGuideDTO(
            RecycleGuideFavorite favorite) {
        String name = favorite.getRecycleGuide().getName();
        return RecycleGuideFavoriteResponseDTO.FavoriteGuideDTO.builder()
                .guideId(favorite.getRecycleGuide().getId())
                .name(name)
                .title(name + " 쉽게 배출하는 법")
                .favoritedAt(favorite.getCreatedAt().toLocalDate())
                .build();
    }

    public static RecycleGuideFavoriteResponseDTO.FavoriteGuideListDTO toFavoriteGuideListDTO(
            List<RecycleGuideFavorite> favorites) {
        List<RecycleGuideFavoriteResponseDTO.FavoriteGuideDTO> dtos = favorites.stream()
                .map(RecycleGuideFavoriteConverter::toFavoriteGuideDTO)
                .toList();
        return RecycleGuideFavoriteResponseDTO.FavoriteGuideListDTO.builder()
                .favorites(dtos)
                .build();
    }
}
