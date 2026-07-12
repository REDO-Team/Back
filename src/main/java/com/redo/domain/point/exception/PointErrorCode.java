package com.redo.domain.point.exception;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PointErrorCode implements BaseErrorCode {

    INVALID_POINT_AMOUNT(HttpStatus.BAD_REQUEST,
            "POINT_400_001",
            "포인트 금액은 0보다 커야 합니다."),

    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST,
            "POINT_400_002",
            "포인트 거래 멱등키가 올바르지 않습니다."),

    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST,
            "POINT_400_003",
            "보유 포인트가 부족합니다.");

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
