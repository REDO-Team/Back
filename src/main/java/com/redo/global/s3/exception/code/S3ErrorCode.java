package com.redo.global.s3.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {

    EMPTY_FILE(HttpStatus.BAD_REQUEST,
            "S3_400_001",
            "업로드할 이미지가 비어 있습니다."),

    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST,
            "S3_400_002",
            "지원하지 않는 이미지 형식입니다."),

    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE,
            "S3_413_001",
            "이미지는 10MB 이하만 업로드할 수 있습니다."),

    INVALID_DIRECTORY(HttpStatus.BAD_REQUEST,
            "S3_400_003",
            "이미지 저장 경로가 올바르지 않습니다."),

    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST,
            "S3_400_004",
            "이미지 객체 키가 올바르지 않습니다."),

    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "S3_500_001",
            "이미지 업로드에 실패했습니다."),

    DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "S3_500_002",
            "이미지 삭제에 실패했습니다."),

    URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "S3_500_003",
            "이미지 URL 생성에 실패했습니다.");

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
