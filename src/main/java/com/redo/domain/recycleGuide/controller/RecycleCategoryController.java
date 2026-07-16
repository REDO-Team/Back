package com.redo.domain.recycleGuide.controller;

import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.service.RecycleCategoryService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "분리수거 카테고리", description = "분리수거 카테고리 및 품목 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recycle-categories")
public class RecycleCategoryController {

    private final RecycleCategoryService recycleCategoryService;

    @Operation(summary = "카테고리 및 품목 조회", description = "모든 카테고리와 해당 카테고리에 속한 배출 가이드의 품목명 및 식별자를 반환합니다.")
    @GetMapping
    public ApiResponse<List<RecycleGuideResponseDTO.CategoryWithGuidesDTO>> getCategories() {
        List<RecycleGuideResponseDTO.CategoryWithGuidesDTO> result =
                recycleCategoryService.getAllCategoriesWithGuides();
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }
}
