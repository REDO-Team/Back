package com.redo.domain.user.controller;

import com.redo.domain.user.dto.AuthReqDTO;
import com.redo.domain.user.dto.AuthResDTO;
import com.redo.domain.user.service.AuthService;
import com.redo.domain.user.converter.AuthConverter;
import com.redo.domain.user.exception.AuthSuccessCode;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<AuthResDTO.Login> login(
            @RequestBody @Valid AuthReqDTO.Login request,
            HttpServletResponse response
    ) {
        // TokenResult(user, accessToken, refreshToken) 받기
        AuthService.TokenResult result = authService.login(request.loginId(), request.password());

        // 2. refreshToken을 쿠키로 만들어서 응답 헤더에 추가
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(result.refreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 3. accessToken + user 정보로 Response DTO 생성
        AuthResDTO.Login responseBody = AuthConverter.toLoginResponse(result.user(), result.accessToken());

        // 4. 공통 응답 포맷으로 감싸서 반환
        return ApiResponse.onSuccess(AuthSuccessCode.LOGIN_SUCCESS, responseBody);
    }

    @PostMapping("/reissue")
    public ApiResponse<AuthResDTO.Reissue> reissue(
            @CookieValue("refreshToken") String refreshToken
    ) {
        AuthResDTO.Reissue result = authService.reissue(refreshToken);
        return ApiResponse.onSuccess(AuthSuccessCode.TOKEN_REISSUE_SUCCESS, result);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response
    ){
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(accessToken);
        authService.logout(userId);
        // 쿠키 삭제 (Max-Age=0)
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ApiResponse.onSuccess(AuthSuccessCode.LOGOUT_SUCCESS, null);
    }
}