package com.redo.domain.certification.exception.code;

import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CertificationSuccessCode implements BaseSuccessCode {

    GET_HOME_SUCCESS(
            HttpStatus.OK,
            "CERTIFICATION200_0",
            "인증하기 화면 조회에 성공했습니다."
    ),
    GET_HOME_DAILY_LIMIT_EXCEEDED(
            HttpStatus.OK,
            "CERTIFICATION200_1",
            "오늘 포인트 적립 가능 횟수를 모두 사용했습니다."
    ),
    GET_HOME_COOLDOWN(
            HttpStatus.OK,
            "CERTIFICATION200_8",
            "이전 인증 후 5분이 지나야 다시 인증할 수 있습니다."
    ),
    GET_HOME_PROCESSING_EXISTS(
            HttpStatus.OK,
            "CERTIFICATION200_9",
            "진행 중인 인증이 있습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public static CertificationSuccessCode from(CertificationRestrictionType restrictionType) {
        return switch (restrictionType) {
            case NONE -> GET_HOME_SUCCESS;
            case DAILY_LIMIT_EXCEEDED -> GET_HOME_DAILY_LIMIT_EXCEEDED;
            case COOLDOWN -> GET_HOME_COOLDOWN;
            case PROCESSING_EXISTS -> GET_HOME_PROCESSING_EXISTS;
        };
    }

    @Override
    public SuccessReason getReason() {
        return SuccessReason.builder()
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}
