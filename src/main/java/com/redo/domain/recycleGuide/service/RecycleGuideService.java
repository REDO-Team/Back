package com.redo.domain.recycleGuide.service;

import com.redo.domain.recycleGuide.converter.RecycleGuideConverter;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.entity.RecycleGuide;
import com.redo.domain.recycleGuide.exception.RecycleGuideErrorCode;
import com.redo.domain.recycleGuide.repository.RecycleGuideRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleGuideService {

    private final RecycleGuideRepository recycleGuideRepository;

    public RecycleGuideResponseDTO.GuideDetailDTO getGuideByName(String name) {
        RecycleGuide guide = recycleGuideRepository.findByName(name)
                .orElseThrow(() -> new GeneralException(RecycleGuideErrorCode.GUIDE_NOT_FOUND));

        return RecycleGuideConverter.toGuideDetailDTO(guide);
    }
}
