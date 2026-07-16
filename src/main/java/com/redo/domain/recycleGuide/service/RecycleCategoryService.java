package com.redo.domain.recycleGuide.service;

import com.redo.domain.recycleGuide.converter.RecycleCategoryConverter;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.entity.RecycleCategory;
import com.redo.domain.recycleGuide.repository.RecycleCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecycleCategoryService {

    private final RecycleCategoryRepository recycleCategoryRepository;

    public List<RecycleGuideResponseDTO.CategoryWithGuidesDTO> getAllCategoriesWithGuides() {
        List<RecycleCategory> categories = recycleCategoryRepository.findAllWithGuides();
        return RecycleCategoryConverter.toCategoryWithGuidesDTOList(categories);
    }
}
