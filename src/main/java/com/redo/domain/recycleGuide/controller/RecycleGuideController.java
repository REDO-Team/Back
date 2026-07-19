package com.redo.domain.recycleGuide.controller;

import com.redo.domain.recycleGuide.dto.RecycleGuideResponseDTO;
import com.redo.domain.recycleGuide.service.RecycleGuideService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배출 가이드", description = "배출 가이드 상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/guides")
public class RecycleGuideController {

    private final RecycleGuideService recycleGuideService;

    @Operation(summary = "품목명으로 배출 가이드 조회",
               description = "품목명(name)을 쿼리 파라미터로 전달하면 해당 배출 가이드의 상세 정보를 반환합니다.")
    @GetMapping
    public ApiResponse<RecycleGuideResponseDTO.GuideDetailDTO> getGuideByName(
            @Parameter(description = "조회할 품목명", example = "투명 페트병", required = true)
            @RequestParam String name) {

        RecycleGuideResponseDTO.GuideDetailDTO result = recycleGuideService.getGuideByName(name);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, result);
    }
}
