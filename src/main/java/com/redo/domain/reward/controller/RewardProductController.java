package com.redo.domain.reward.controller;

import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPageResponseDTO;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.exception.code.RewardSuccessCode;
import com.redo.domain.reward.service.RewardProductService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.util.CursorRequestValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards/products")
@Tag(name = "리워드 상품", description = "리워드 상품 조회, 구매 및 구매 내역 API")
public class RewardProductController {

    private final RewardProductService rewardProductService;

    @GetMapping
    @Operation(
            summary = "리워드 상품 목록 조회",
            description = "판매 가능한 리워드 상품을 필요한 포인트가 낮은 순으로 커서 기반 유형별 조회합니다."
    )
    public ApiResponse<RewardProductPageResponseDTO> getRewardProducts(
            @RequestParam(required = false) RewardProductType rewardProductType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        CursorRequestValidator.validate(
                cursor,
                size,
                () -> new RewardException(RewardErrorCode.INVALID_CURSOR_REQUEST)
        );

        return ApiResponse.onSuccess(
                RewardSuccessCode.GET_REWARD_PRODUCTS_SUCCESS,
                rewardProductService.getRewardProducts(rewardProductType, cursor, size)
        );
    }

    @GetMapping("/{rewardProductId}")
    @Operation(summary = "리워드 상품 상세 조회", description = "판매 가능한 리워드 상품의 상세 정보를 조회합니다.")
    public ApiResponse<RewardProductDetailResponseDTO> getRewardProduct(
            @PathVariable Long rewardProductId
    ) {
        return ApiResponse.onSuccess(
                RewardSuccessCode.GET_REWARD_PRODUCT_SUCCESS,
                rewardProductService.getRewardProduct(rewardProductId)
        );
    }
}
