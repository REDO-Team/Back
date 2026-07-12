package com.redo.domain.user.exception;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER_404_001",
            "사용자를 찾을 수 없습니다."),

    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER_404_002",
            "사용자 프로필을 찾을 수 없습니다."),

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT,
            "USER_409_001",
            "이미 사용 중인 아이디입니다."),

    DUPLICATE_EMAIL(HttpStatus.CONFLICT,
            "USER_409_002",
            "이미 사용 중인 이메일입니다."),

    WITHDRAWAL_REASON_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER_404_003",
            "탈퇴 사유를 찾을 수 없습니다."),

    ALREADY_WITHDRAWN_USER(HttpStatus.BAD_REQUEST,
            "USER_400_001",
            "이미 탈퇴한 사용자입니다.");

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
