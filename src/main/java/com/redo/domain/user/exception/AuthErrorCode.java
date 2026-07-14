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
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_004", "유효하지 않거나 만료된 Refresh Token입니다. 다시 로그인해주세요."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_005", "유효하지 않은 Access Token입니다."),

    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_001", "올바른 이메일 형식이 아닙니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH_409_001", "이미 가입된 이메일입니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_400_002", "인증번호가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_400_003", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH_404_001", "인증 요청 내역이 없습니다."),

    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH_400_004", "필수 약관에 모두 동의해야 합니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_400_005", "이메일 인증을 완료해주세요."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "AUTH_409_002", "이미 사용 중인 아이디입니다."),
    DUPLICATE_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "AUTH_409_003", "이미 가입된 계정입니다."),

    VERIFICATION_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_429_001", "인증 시도 횟수를 초과했습니다. 다시 요청해주세요."),
    EMAIL_REQUEST_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "AUTH_429_002", "잠시 후 다시 시도해주세요."),
    LOGIN_ATTEMPT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH_429_003", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),

    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "AUTH_400_006", "올바른 아이디 형식이 아닙니다.");


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