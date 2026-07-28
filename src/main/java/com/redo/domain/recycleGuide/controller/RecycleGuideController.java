package com.redo.domain.recycleGuide.controller;

import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.dto.RecycleGuideRequestDTO;
import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.service.RecycleGuideFavoriteService;
import com.redo.domain.recycleGuide.service.RecycleGuideService;
import com.redo.domain.recycleGuide.service.RecycleGuideAiService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "배출 가이드", description = "배출 가이드 조회, 즐겨찾기 API")
@RestController
@RequiredArgsConstructor
public class RecycleGuideController {

    private final RecycleGuideService recycleGuideService;
    private final RecycleGuideFavoriteService recycleGuideFavoriteService;
    private final RecycleGuideAiService recycleGuideAiService;

    @Operation(summary = "품목명으로 배출 가이드 조회",
               description = "품목명(name)을 쿼리 파라미터로 전달하면 해당 배출 가이드의 상세 정보를 반환합니다.")
    @GetMapping("/api/guides")
    public ApiResponse<RecycleGuideResponseDTO.GuideDetailDTO> getGuideByName(
            @Parameter(description = "조회할 품목명", example = "투명 페트병", required = true)
            @RequestParam String name) {

        RecycleGuideResponseDTO.GuideDetailDTO result = recycleGuideService.getGuideByName(name);
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

    @Operation(summary = "이미지로 배출 가이드 검색", description = "쓰레기 이미지를 업로드하면 AI가 분석하여 해당하는 배출 가이드 상세 정보를 반환합니다.")
    @PostMapping(value = "/api/guides/search-by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RecycleGuideResponseDTO.AiSearchResultDTO> searchGuideByImage(
            @Parameter(description = "분석할 쓰레기 이미지 파일", required = true)
            @RequestPart("image") MultipartFile image) {

        RecycleGuideResponseDTO.AiSearchResultDTO result = recycleGuideAiService.searchGuideByImage(image);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }

    @Operation(summary = "텍스트로 배출 가이드 검색", description = "문제 상황 텍스트를 입력하면 AI가 분석하여 해당하는 배출 가이드 상세 정보를 반환합니다.")
    @PostMapping("/api/guides/search-by-text")
    public ApiResponse<RecycleGuideResponseDTO.AiSearchResultDTO> searchGuideByText(
            @Valid @RequestBody RecycleGuideRequestDTO.TextSearchDTO request) {

        RecycleGuideResponseDTO.AiSearchResultDTO result = recycleGuideAiService.searchGuideByText(request.getQuery());
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }
}
