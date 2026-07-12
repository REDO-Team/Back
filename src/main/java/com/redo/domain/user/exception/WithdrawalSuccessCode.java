package com.redo.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;

@Getter
@AllArgsConstructor
public enum WithdrawalSuccessCode implements BaseSuccessCode {

    GET_WITHDRAWAL_REASONS_SUCCESS(HttpStatus.OK, "WITHDRAWAL_200_001", "탈퇴 사유 목록 조회 성공하였습니다"),
    WITHDRAWAL_SUCCESS(HttpStatus.OK, "WITHDRAWAL_200_002", "회원 탈퇴 처리 성공하였습니다");

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