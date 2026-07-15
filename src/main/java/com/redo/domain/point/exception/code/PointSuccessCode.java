package com.redo.domain.point.exception.code;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PointSuccessCode implements BaseSuccessCode {

    GET_POINT_SUCCESS(HttpStatus.OK,
            "POINT_200_001",
            "포인트 조회에 성공했습니다."),

    GET_POINT_TRANSACTIONS_SUCCESS(HttpStatus.OK,
            "POINT_200_002",
            "포인트 거래 내역 조회에 성공했습니다.");

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
