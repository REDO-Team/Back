package com.redo.domain.certification.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CertificationErrorCode implements BaseErrorCode {

    INVALID_SOURCE_GUIDE_CONTRACT(
            HttpStatus.BAD_REQUEST,
            "CERTIFICATION400_1",
            "인증 방식과 배출 가이드 요청값이 올바르지 않습니다."
    ),
    RECYCLE_GUIDE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CERTIFICATION404_1",
            "선택한 배출 가이드를 찾을 수 없습니다."
    ),
    PROCESSING_EXISTS(
            HttpStatus.CONFLICT,
            "CERTIFICATION409_0",
            "진행 중인 인증이 있습니다."
    ),
    ACTIVE_TEMPLATE_NOT_FOUND(
            HttpStatus.PRECONDITION_FAILED,
            "CERTIFICATION412_0",
            "선택한 배출 가이드의 활성 판정 템플릿이 없습니다."
    ),
    DAILY_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "CERTIFICATION429_0",
            "오늘 포인트 적립 가능 횟수를 모두 사용했습니다."
    ),
    COOLDOWN(
            HttpStatus.TOO_MANY_REQUESTS,
            "CERTIFICATION429_1",
            "이전 인증 완료 후 5분이 지나야 새로운 인증을 시작할 수 있습니다."
    ),
    IMAGE_READ_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "CERTIFICATION500_0",
            "인증 이미지를 불러오지 못했습니다."
    ),
    JUDGEMENT_PIPELINE_NOT_READY(
            HttpStatus.SERVICE_UNAVAILABLE,
            "CERTIFICATION503_0",
            "인증 판정 기능이 아직 준비되지 않았습니다."
    ),
    JUDGEMENT_OVERLOADED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "CERTIFICATION503_1",
            "인증 요청이 많아 잠시 후 다시 시도해 주세요."
    );

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
