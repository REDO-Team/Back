package com.redo.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_002", "로그인에 성공했습니다."),
    SOCIAL_LOGIN_SUCCESS(HttpStatus.OK, "AUTH_200_003", "소셜 로그인에 성공했습니다."),
    TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "AUTH_200_004", "토큰 재발급에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "AUTH_200_005", "로그아웃에 성공했습니다."),
    CHECK_LOGIN_ID_SUCCESS(HttpStatus.OK, "AUTH_200_006", "아이디 중복 확인에 성공했습니다."),

    EMAIL_VERIFY_REQUEST_SUCCESS(HttpStatus.OK, "AUTH_200_007", "인증번호가 발송되었습니다."),
    EMAIL_VERIFY_CONFIRM_SUCCESS(HttpStatus.OK, "AUTH_200_008", "이메일 인증이 완료되었습니다."),

    SIGNUP_SUCCESS(HttpStatus.CREATED, "AUTH_201_001", "회원가입에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public SuccessReason getReason() {
        return SuccessReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}