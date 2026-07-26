package com.redo.domain.point.controller;

import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionPageResponseDTO;
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
import com.redo.domain.point.exception.code.PointSuccessCode;
import com.redo.domain.point.service.PointService;
import com.redo.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards/points")
@Tag(name = "포인트", description = "사용자 포인트 및 거래 내역 조회 API")
public class PointController {

    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final PointService pointService;

    @GetMapping
    @Operation(summary = "보유 포인트 조회", description = "로그인한 사용자의 보유 포인트와 이번 달 적립 포인트를 조회합니다.")
    public ApiResponse<PointBalanceResponseDTO> getMyPoint(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_SUCCESS, pointService.getMyPoint(userId));
    }

    @GetMapping("/transactions")
    @Operation(
            summary = "포인트 거래 내역 조회",
            description = "로그인한 사용자의 포인트 적립 및 사용 내역을 커서 기반으로 최신순 조회합니다."
    )
    public ApiResponse<PointTransactionPageResponseDTO> getMyPointTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        validateCursorRequest(cursor, size);

        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_TRANSACTIONS_SUCCESS,
                pointService.getMyPointTransactions(userId, cursor, size)
        );
    }

    private void validateCursorRequest(Long cursor, int size) {
        if ((cursor != null && cursor <= 0)
                || size < MIN_PAGE_SIZE
                || size > MAX_PAGE_SIZE) {
            throw new PointException(PointErrorCode.INVALID_CURSOR_REQUEST);
        }
    }
}
