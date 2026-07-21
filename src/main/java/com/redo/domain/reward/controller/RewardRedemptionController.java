package com.redo.domain.reward.controller;

import com.redo.domain.reward.converter.RewardRedemptionConverter;
import com.redo.domain.reward.dto.req.RewardRedemptionCreateRequestDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionHistoryPageResponseDTO;
import com.redo.domain.reward.dto.res.RewardRedemptionResponseDTO;
import com.redo.domain.reward.exception.RewardException;
import com.redo.domain.reward.exception.code.RewardErrorCode;
import com.redo.domain.reward.exception.code.RewardSuccessCode;
import com.redo.domain.reward.service.RewardRedemptionService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards/redemptions")
@Tag(name = "리워드 상품", description = "리워드 상품 조회, 구매 및 구매 내역 API")
public class RewardRedemptionController {

    private static final int MIN_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final RewardRedemptionService rewardRedemptionService;

    @PostMapping
    @Operation(
            summary = "리워드 상품 구매",
            description = "포인트와 재고를 차감하고 상품 구매 및 포인트 사용 내역을 생성합니다. "
                    + "배송 상품은 shippingAddressId, 기프티콘은 receiverName과 receiverPhone이 필요합니다."
    )
    public ApiResponse<RewardRedemptionResponseDTO> redeem(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "중복 구매 방지를 위한 요청 고유 키", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RewardRedemptionCreateRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                RewardSuccessCode.CREATE_REWARD_REDEMPTION_SUCCESS,
                rewardRedemptionService.redeem(userId, idempotencyKey, request)
        );
    }

    @GetMapping
    @Operation(
            summary = "리워드 상품 구매 내역 조회",
            description = "로그인한 사용자의 배송 상품과 기프티콘 구매 내역을 최신순으로 조회합니다."
    )
    public ApiResponse<RewardRedemptionHistoryPageResponseDTO> getMyRedemptions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageRequest(page, size);
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.onSuccess(
                RewardSuccessCode.GET_REWARD_REDEMPTIONS_SUCCESS,
                RewardRedemptionConverter.toRewardRedemptionHistoryPageResponse(
                        rewardRedemptionService.getMyRedemptions(userId, pageable)
                )
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new RewardException(RewardErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
