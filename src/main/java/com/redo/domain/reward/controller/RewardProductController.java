package com.redo.domain.reward.controller;

import com.redo.domain.reward.converter.RewardProductConverter;
import com.redo.domain.reward.dto.res.RewardProductDetailResponseDTO;
import com.redo.domain.reward.dto.res.RewardProductPageResponseDTO;
import com.redo.domain.reward.enums.RewardProductType;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.exception.code.RewardSuccessCode;
import com.redo.domain.reward.service.RewardProductService;
import com.redo.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards/products")
public class RewardProductController {

    private static final int MIN_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final RewardProductService rewardProductService;

    // 상품 목록 조회 API
    @GetMapping
    public ApiResponse<RewardProductPageResponseDTO> getRewardProducts(
            @RequestParam(required = false) RewardProductType rewardProductType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );

        return ApiResponse.onSuccess(
                RewardSuccessCode.GET_REWARD_PRODUCTS_SUCCESS,
                RewardProductConverter.toRewardProductPageResponse(
                        rewardProductService.getRewardProducts(rewardProductType, pageable)
                )
        );
    }

    // 상품 상세 조회 API
    @GetMapping("/{rewardProductId}")
    public ApiResponse<RewardProductDetailResponseDTO> getRewardProduct(
            @PathVariable Long rewardProductId
    ) {
        return ApiResponse.onSuccess(
                RewardSuccessCode.GET_REWARD_PRODUCT_SUCCESS,
                rewardProductService.getRewardProduct(rewardProductId)
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new RewardException(RewardErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
