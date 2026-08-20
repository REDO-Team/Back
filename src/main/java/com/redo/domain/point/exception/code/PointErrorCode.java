package com.redo.domain.point.exception.code;

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
            "보유 포인트가 부족합니다."),

    POINT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "POINT_400_004",
            "보유 포인트 한도를 초과할 수 없습니다."),

    INVALID_CURSOR_REQUEST(HttpStatus.BAD_REQUEST,
            "POINT_400_005",
            "커서 또는 조회 개수가 올바르지 않습니다."),

    INVALID_CERTIFICATION_SOURCE(HttpStatus.BAD_REQUEST,
            "POINT_400_006",
            "인증 경로가 올바르지 않습니다."),

    // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // DAILY_EARN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
    //         "POINT_400_007",
    //         "하루 최대 3번까지 포인트를 적립할 수 있습니다."),

    CERTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,
            "POINT_404_001",
            "포인트 적립 대상 인증을 찾을 수 없습니다.");

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
