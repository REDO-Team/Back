package com.redo.domain.user.exception; // 또는 팀 컨벤션에 맞는 위치

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_LOGIN_ID_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "아이디 또는 비밀번호가 일치하지 않습니다."),
    WITHDRAWN_ACCOUNT(HttpStatus.FORBIDDEN, "AUTH_403_002", "탈퇴한 계정입니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_003", "유효하지 않은 소셜 인증 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_004", "유효하지 않거나 만료된 Refresh Token입니다. 다시 로그인해주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReason getReason() {
        return ErrorReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}