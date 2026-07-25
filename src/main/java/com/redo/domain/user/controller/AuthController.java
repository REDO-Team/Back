package com.redo.domain.user.controller;

import com.redo.domain.user.dto.AuthReqDTO;
import com.redo.domain.user.dto.AuthResDTO;
import com.redo.domain.user.exception.AuthErrorCode;
import com.redo.domain.user.service.AuthService;
import com.redo.domain.user.converter.AuthConverter;
import com.redo.domain.user.exception.AuthSuccessCode;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.security.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Tag(name = "회원가입, 로그인", description = "회원가입, 로그인 및 토큰 관리 API")
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
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response

    ) {
        AuthService.TokenResult result = authService.reissue(refreshToken);
        // 새 refreshToken을 쿠키로 갱신
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(result.refreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        AuthResDTO.Reissue responseBody = new AuthResDTO.Reissue(result.accessToken());

        return ApiResponse.onSuccess(AuthSuccessCode.TOKEN_REISSUE_SUCCESS, responseBody);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletResponse response
    ){
        String accessToken = authHeader.replace("Bearer ", "");
        Long userId ;
        try {
            userId = jwtUtil.getUserIdFromToken(accessToken);
        } catch (Exception e) {
            throw new GeneralException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

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

    @PostMapping("/login/google")
    public ApiResponse<AuthResDTO.SocialLogin> loginGoogle(
            @RequestBody @Valid AuthReqDTO.SocialLogin request,
            HttpServletResponse response
    ) {
        AuthService.SocialLoginResult socialLoginResult = authService.socialLogin("GOOGLE", request.accessToken());
        // 기존회원경우 ( 신규회원은 건너뜀)
        if (!socialLoginResult.isNewUser()) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", socialLoginResult.refreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtUtil.getRefreshTokenExpiration()))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }
        AuthResDTO.SocialLogin responseBody = AuthConverter.toSocialLoginResponse(
                socialLoginResult.user(),
                socialLoginResult.isNewUser(),
                socialLoginResult.accessToken(),
                socialLoginResult.socialProvider(),
                socialLoginResult.socialId()
        );
        return ApiResponse.onSuccess(AuthSuccessCode.SOCIAL_LOGIN_SUCCESS, responseBody);
    }


    @PostMapping("/login/kakao")
    public ApiResponse<AuthResDTO.SocialLogin> loginKakao(
            @RequestBody @Valid AuthReqDTO.SocialLogin request,
            HttpServletResponse response
    ) {
        AuthService.SocialLoginResult socialLoginResult = authService.socialLogin("KAKAO", request.accessToken());
        // 기존회원경우 ( 신규회원은 건너뜀)
        if (!socialLoginResult.isNewUser()) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", socialLoginResult.refreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtUtil.getRefreshTokenExpiration()))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }
        AuthResDTO.SocialLogin responseBody = AuthConverter.toSocialLoginResponse(
                socialLoginResult.user(),
                socialLoginResult.isNewUser(),
                socialLoginResult.accessToken(),
                socialLoginResult.socialProvider(),
                socialLoginResult.socialId()
        );
        return ApiResponse.onSuccess(AuthSuccessCode.SOCIAL_LOGIN_SUCCESS, responseBody);
    }

    @PostMapping("/login/naver")
    public ApiResponse<AuthResDTO.SocialLogin> loginNaver(
            @RequestBody @Valid AuthReqDTO.SocialLogin request,
            HttpServletResponse response
    ) {
        AuthService.SocialLoginResult socialLoginResult = authService.socialLogin("NAVER", request.accessToken());
        // 기존회원경우 ( 신규회원은 건너뜀)
        if (!socialLoginResult.isNewUser()) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", socialLoginResult.refreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtUtil.getRefreshTokenExpiration()))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }
        AuthResDTO.SocialLogin responseBody = AuthConverter.toSocialLoginResponse(
                socialLoginResult.user(),
                socialLoginResult.isNewUser(),
                socialLoginResult.accessToken(),
                socialLoginResult.socialProvider(),
                socialLoginResult.socialId()
        );
        return ApiResponse.onSuccess(AuthSuccessCode.SOCIAL_LOGIN_SUCCESS, responseBody);
    }

    @GetMapping("/login-id/check")
    public ApiResponse<AuthResDTO.LoginIdCheck> checkLoginId(
            @RequestParam String loginId
    ) {
        AuthResDTO.LoginIdCheck result = authService.checkLoginId(loginId);
        return ApiResponse.onSuccess(AuthSuccessCode.CHECK_LOGIN_ID_SUCCESS, result);
    }

    @PostMapping("/email/verify-request")
    public ApiResponse<Void> sendVerificationEmail(
            @RequestBody @Valid AuthReqDTO.EmailVerifyRequest request
    ) {
        authService.sendVerificationEmail(request.email());
        return ApiResponse.onSuccess(AuthSuccessCode.EMAIL_VERIFY_REQUEST_SUCCESS, null);
    }


    @PostMapping("/email/verify-confirm")
    public ApiResponse<Void> confirmVerificationCode(
            @RequestBody @Valid AuthReqDTO.EmailVerifyConfirm request
    ) {
        authService.confirmVerificationCode(request.email(), request.code());
        return ApiResponse.onSuccess(AuthSuccessCode.EMAIL_VERIFY_CONFIRM_SUCCESS, null);
    }

    @PostMapping("/signup")
    public ApiResponse<AuthResDTO.Signup> signup(
            @RequestBody @Valid AuthReqDTO.Signup request,
            HttpServletResponse response
    ) {
        // 1) refreshToken 쿠키 설정
        AuthService.TokenResult result = authService.signup(request);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(result.refreshTokenExpiration()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        AuthResDTO.Signup responseBody = new AuthResDTO.Signup(result.user().getId(), result.accessToken());

        return ApiResponse.onSuccess(AuthSuccessCode.SIGNUP_SUCCESS, responseBody);
    }
}
