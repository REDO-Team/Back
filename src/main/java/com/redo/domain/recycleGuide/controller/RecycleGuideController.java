package com.redo.domain.recycleGuide.controller;

import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.service.RecycleCategoryService;
import com.redo.domain.recycleGuide.service.RecycleGuideFavoriteService;
import com.redo.domain.recycleGuide.service.RecycleGuideService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "배출 가이드", description = "배출 가이드 조회, 카테고리, 즐겨찾기 API")
@RestController
@RequiredArgsConstructor
public class RecycleGuideController {

    private final RecycleGuideService recycleGuideService;
    private final RecycleCategoryService recycleCategoryService;
    private final RecycleGuideFavoriteService recycleGuideFavoriteService;

    @Operation(summary = "품목명으로 배출 가이드 조회",
               description = "품목명(name)을 쿼리 파라미터로 전달하면 해당 배출 가이드의 상세 정보를 반환합니다.")
    @GetMapping("/api/guides")
    public ApiResponse<RecycleGuideResponseDTO.GuideDetailDTO> getGuideByName(
            @Parameter(description = "조회할 품목명", example = "투명 페트병", required = true)
            @RequestParam String name) {

        RecycleGuideResponseDTO.GuideDetailDTO result = recycleGuideService.getGuideByName(name);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }

    @Operation(summary = "카테고리 및 품목 조회", description = "모든 카테고리와 해당 카테고리에 속한 배출 가이드의 품목명 및 식별자를 반환합니다.")
    @GetMapping("/api/recycle-categories")
    public ApiResponse<List<RecycleGuideResponseDTO.CategoryWithGuidesDTO>> getCategories() {
        List<RecycleGuideResponseDTO.CategoryWithGuidesDTO> result =
                recycleCategoryService.getAllCategoriesWithGuides();
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }

    @Operation(summary = "가이드 즐겨찾기 추가", description = "특정 배출 가이드를 사용자의 즐겨찾기에 추가합니다.")
    @PostMapping("/api/guides/{guideId}/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecycleGuideFavoriteResponseDTO.FavoriteResultDTO> addFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long guideId) {

        RecycleGuideFavoriteResponseDTO.FavoriteResultDTO result =
                recycleGuideFavoriteService.addFavorite(userId, guideId);

        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, result);
    }
}
