package com.redo.domain.term.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;

@Getter
@AllArgsConstructor
public enum TermErrorCode implements BaseErrorCode {

    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "TERM_404_001", "등록된 약관이 없습니다.");


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