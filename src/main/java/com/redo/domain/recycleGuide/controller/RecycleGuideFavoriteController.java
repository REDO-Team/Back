package com.redo.domain.recycleGuide.controller;

import com.redo.domain.recycleGuide.dto.RecycleGuideFavoriteResponseDTO;
import com.redo.domain.recycleGuide.service.RecycleGuideFavoriteService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "가이드 즐겨찾기", description = "배출 가이드 즐겨찾기 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guides")
public class RecycleGuideFavoriteController {

    private final RecycleGuideFavoriteService recycleGuideFavoriteService;

    @Operation(summary = "가이드 즐겨찾기 추가", description = "특정 배출 가이드를 사용자의 즐겨찾기에 추가합니다.")
    @PostMapping("/{guideId}/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecycleGuideFavoriteResponseDTO.FavoriteResultDTO> addFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long guideId) {

        RecycleGuideFavoriteResponseDTO.FavoriteResultDTO result =
                recycleGuideFavoriteService.addFavorite(userId, guideId);

        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, result);
    }
}
