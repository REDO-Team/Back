package com.redo.domain.recycleGuide.exception;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecycleGuideErrorCode implements BaseErrorCode {

    GUIDE_NOT_FOUND(HttpStatus.NOT_FOUND, "GUIDE_404_001", "해당 배출 가이드를 찾을 수 없습니다."),
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "GUIDE_409_001", "이미 즐겨찾기에 추가된 가이드입니다.");

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
