package com.redo.global.ai.gemini.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeminiErrorCode implements BaseErrorCode {

    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "GEMINI_400_001",
            "Gemini 요청 값이 올바르지 않습니다."
    ),
    INVALID_RESPONSE_SCHEMA(
            HttpStatus.BAD_REQUEST,
            "GEMINI_400_002",
            "Gemini 응답 스키마가 올바르지 않습니다."
    ),
    EMPTY_FILE(
            HttpStatus.BAD_REQUEST,
            "GEMINI_400_003",
            "분석할 이미지 파일이 비어 있습니다."
    ),
    INVALID_IMAGE_TYPE(
            HttpStatus.BAD_REQUEST,
            "GEMINI_400_004",
            "지원하지 않는 이미지 형식입니다."
    ),
    FILE_SIZE_EXCEEDED(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "GEMINI_413_001",
            "이미지는 10MB 이하만 분석할 수 있습니다."
    ),
    AUTHENTICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "GEMINI_401_001",
            "Gemini 인증 설정을 확인해 주세요."
    ),
    RATE_LIMITED(
            HttpStatus.TOO_MANY_REQUESTS,
            "GEMINI_429_001",
            "Gemini 사용량 제한으로 요청을 처리할 수 없습니다."
    ),
    EMPTY_OR_INVALID_RESPONSE(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "GEMINI_500_001",
            "Gemini 응답을 처리할 수 없습니다."
    ),
    PROVIDER_ERROR(
            HttpStatus.BAD_GATEWAY,
            "GEMINI_502_001",
            "Gemini 서비스 호출에 실패했습니다."
    ),
    CLIENT_DISABLED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "GEMINI_503_001",
            "현재 Gemini 기능을 사용할 수 없습니다."
    ),
    TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "GEMINI_504_001",
            "Gemini 응답 시간이 초과되었습니다."
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
