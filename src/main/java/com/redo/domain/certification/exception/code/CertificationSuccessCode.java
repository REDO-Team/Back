package com.redo.domain.certification.exception.code;

import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationStatus;
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
    // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // GET_HOME_DAILY_LIMIT_EXCEEDED(
    //         HttpStatus.OK,
    //         "CERTIFICATION200_1",
    //         "오늘 포인트 적립 가능 횟수를 모두 사용했습니다."
    // ),
    // GET_HOME_COOLDOWN(
    //         HttpStatus.OK,
    //         "CERTIFICATION200_8",
    //         "이전 성공 인증 후 5분이 지나야 다시 인증할 수 있습니다."
    // ),
    GET_HOME_PROCESSING_EXISTS(
            HttpStatus.OK,
            "CERTIFICATION200_9",
            "진행 중인 인증이 있습니다."
    ),
    CREATE_CERTIFICATION_PASSED(
            HttpStatus.CREATED,
            "CERTIFICATION201_0",
            "인증 검수가 완료되었습니다."
    ),
    CREATE_CERTIFICATION_FAILED(
            HttpStatus.CREATED,
            "CERTIFICATION201_1",
            "인증 검수가 완료되었습니다."
    ),
    RETRY_CERTIFICATION_PASSED(
            HttpStatus.OK,
            "CERTIFICATION200_10",
            "재검수가 완료되었습니다."
    ),
    RETRY_CERTIFICATION_FAILED(
            HttpStatus.OK,
            "CERTIFICATION200_11",
            "재검수가 완료되었습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public static CertificationSuccessCode from(CertificationRestrictionType restrictionType) {
        return switch (restrictionType) {
            case NONE -> GET_HOME_SUCCESS;
            case PROCESSING_EXISTS -> GET_HOME_PROCESSING_EXISTS;
            // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
            // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
            // case DAILY_LIMIT_EXCEEDED -> GET_HOME_DAILY_LIMIT_EXCEEDED;
            // case COOLDOWN -> GET_HOME_COOLDOWN;
        };
    }

    public static CertificationSuccessCode from(CertificationStatus status) {
        return switch (status) {
            case PASSED -> CREATE_CERTIFICATION_PASSED;
            case FAILED -> CREATE_CERTIFICATION_FAILED;
            case PROCESSING -> throw new IllegalArgumentException(
                    "POST certification response must be terminal"
            );
        };
    }

    public static CertificationSuccessCode fromRetry(CertificationStatus status) {
        return switch (status) {
            case PASSED -> RETRY_CERTIFICATION_PASSED;
            case FAILED -> RETRY_CERTIFICATION_FAILED;
            case PROCESSING -> throw new IllegalArgumentException(
                    "POST certification retry response must be terminal"
            );
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
