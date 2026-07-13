package com.redo.domain.point.controller;

import com.redo.domain.point.converter.PointConverter;
import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionPageResponseDTO;
import com.redo.domain.point.exception.PointSuccessCode;
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

    private final PointService pointService;

    @GetMapping
    public ApiResponse<PointBalanceResponseDTO> getMyPoint(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_SUCCESS, pointService.getMyPoint(userId));
    }

    @GetMapping("/transactions")
    public ApiResponse<PointTransactionPageResponseDTO> getMyPointTransactions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        return ApiResponse.onSuccess(PointSuccessCode.GET_POINT_TRANSACTIONS_SUCCESS,
                PointConverter.toPointTransactionPageResponse(pointService.getMyPointTransactions(userId, pageable))
        );
    }
}
