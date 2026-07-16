package com.redo.domain.recycleGuide.converter;

import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.entity.RecycleCategory;
import com.redo.domain.recycleGuide.entity.RecycleGuide;

import java.util.List;

public class RecycleCategoryConverter {

    private RecycleCategoryConverter() {
    }

    public static RecycleGuideResponseDTO.GuideSimpleDTO toGuideSimpleDTO(RecycleGuide guide) {
        return RecycleGuideResponseDTO.GuideSimpleDTO.builder()
                .guideId(guide.getId())
                .guideName(guide.getName())
                .build();
    }

    public static RecycleGuideResponseDTO.CategoryWithGuidesDTO toCategoryWithGuidesDTO(RecycleCategory category) {
        List<RecycleGuideResponseDTO.GuideSimpleDTO> guides = category.getRecycleGuides().stream()
                .map(RecycleCategoryConverter::toGuideSimpleDTO)
                .toList();

        return RecycleGuideResponseDTO.CategoryWithGuidesDTO.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .guides(guides)
                .build();
    }

    public static List<RecycleGuideResponseDTO.CategoryWithGuidesDTO> toCategoryWithGuidesDTOList(
            List<RecycleCategory> categories) {
        return categories.stream()
                .map(RecycleCategoryConverter::toCategoryWithGuidesDTO)
                .toList();
    }
}
