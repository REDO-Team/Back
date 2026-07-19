package com.redo.domain.recycleGuide.converter;

import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.entity.RecycleGuideStep;

import java.util.List;

public class RecycleGuideConverter {

    private RecycleGuideConverter() {
    }

    public static RecycleGuideResponseDTO.GuideDetailDTO toGuideDetailDTO(RecycleGuide guide) {
        List<String> steps = guide.getGuideSteps().stream()
                .map(RecycleGuideStep::getDescription)
                .toList();

        return RecycleGuideResponseDTO.GuideDetailDTO.builder()
                .name(guide.getName())
                .imageUrl(guide.getImageUrl())
                .tip(guide.getTip())
                .recycleCategory(guide.getRecycleCategory().getName())
                .guideSteps(steps)
                .build();
    }
}
