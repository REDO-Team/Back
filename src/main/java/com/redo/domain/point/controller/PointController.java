package com.redo.domain.point.controller;

import com.redo.domain.point.converter.PointConverter;
import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionPageResponseDTO;
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
import com.redo.domain.point.exception.code.PointSuccessCode;
import com.redo.domain.point.service.PointService;
import com.redo.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rewards/points")
public class PointController {

    private static final int MIN_PAGE = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final PointService pointService;

    // 포인트 조회 API
    @GetMapping
    public ApiResponse<PointBalanceResponseDTO> getMyPoint(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_SUCCESS, pointService.getMyPoint(userId));
    }

    // 포인트 거래 내역 조회 API
    @GetMapping("/transactions")
    public ApiResponse<PointTransactionPageResponseDTO> getMyPointTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        validatePageRequest(page, size);

        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_TRANSACTIONS_SUCCESS,
                PointConverter.toPointTransactionPageResponse(pointService.getMyPointTransactions(userId, pageable))
        );
    }

    private void validatePageRequest(int page, int size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new PointException(PointErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
