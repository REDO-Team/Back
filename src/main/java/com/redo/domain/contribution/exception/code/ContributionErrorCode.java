package com.redo.domain.contribution.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ContributionErrorCode implements BaseErrorCode {

    INVALID_CURSOR_REQUEST(
            HttpStatus.BAD_REQUEST,
            "CONTRIBUTION_400_001",
            "커서 또는 조회 개수가 올바르지 않습니다."
    ),

    INVALID_CERTIFICATION_STATUS(
            HttpStatus.BAD_REQUEST,
            "CONTRIBUTION_400_002",
            "성공한 인증만 기여도 이벤트로 등록할 수 있습니다."
    ),

    CONTRIBUTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CONTRIBUTION_404_001",
            "기여도 정보를 찾을 수 없습니다."
    ),

    CERTIFICATION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "CONTRIBUTION_404_002",
            "기여도 이벤트 대상 인증을 찾을 수 없습니다."
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
