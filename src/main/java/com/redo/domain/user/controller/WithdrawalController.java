package com.redo.domain.user.controller;

import com.redo.domain.user.dto.WithdrawalReqDTO;
import com.redo.domain.user.dto.WithdrawalResDTO;
import com.redo.domain.user.exception.AuthErrorCode;
import com.redo.domain.user.exception.WithdrawalSuccessCode;
import com.redo.domain.user.service.WithdrawalService;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final JwtUtil jwtUtil;

    @GetMapping("/withdrawal-reasons")
    public ApiResponse<WithdrawalResDTO.ReasonList> getWithdrawalReasons() {
        WithdrawalResDTO.ReasonList reasonList = withdrawalService.getWithdrawalReasons();

        return ApiResponse.onSuccess(WithdrawalSuccessCode.GET_WITHDRAWAL_REASONS_SUCCESS, reasonList);
    }

    @PostMapping("/me/withdrawal")
    public ApiResponse<Void> withdraw(
            @RequestBody @Valid WithdrawalReqDTO.Withdrawal request,
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response
    ) {
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId;
        try{
            userId = jwtUtil.getUserIdFromToken(accessToken);
        }catch(Exception e){
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        withdrawalService.withdraw(userId, request.reasonId());
        // 쿠키 삭제 (Max-Age=0)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ApiResponse.onSuccess(WithdrawalSuccessCode.WITHDRAWAL_SUCCESS, null);
    }
}