package com.redo.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;

@Getter
@AllArgsConstructor
public enum WithdrawalErrorCode implements BaseErrorCode {

    // 1. 존재하지 않는 탈퇴 사유 (404)
    WITHDRAWAL_REASON_NOT_FOUND(HttpStatus.NOT_FOUND, "WITHDRAWAL_404_001", "존재하지 않는 탈퇴사유입니다"),


    // 2. 이미 탈퇴한 계정 (409, 충돌 상황)
    ALREADY_WITHDRAWN_ACCOUNT(HttpStatus.CONFLICT, "WITHDRAWAL_409_001", "이미 탈퇴한 계정입니다");


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