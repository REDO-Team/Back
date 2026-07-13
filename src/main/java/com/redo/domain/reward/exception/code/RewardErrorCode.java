package com.redo.domain.reward.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RewardErrorCode implements BaseErrorCode {

    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST,
            "REWARD_400_001",
            "페이지 요청 값이 올바르지 않습니다."),

    REWARD_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "REWARD_404_001",
            "리워드 상품을 찾을 수 없습니다.");

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
