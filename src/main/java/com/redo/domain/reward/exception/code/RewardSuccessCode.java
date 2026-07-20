package com.redo.domain.reward.exception.code;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RewardSuccessCode implements BaseSuccessCode {

    GET_REWARD_PRODUCTS_SUCCESS(HttpStatus.OK,
            "REWARD_200_001",
            "리워드 상품 목록 조회에 성공했습니다."),

    GET_REWARD_PRODUCT_SUCCESS(HttpStatus.OK,
            "REWARD_200_002",
            "리워드 상품 상세 조회에 성공했습니다."),

    CREATE_REWARD_REDEMPTION_SUCCESS(HttpStatus.OK,
            "REWARD_200_003",
            "리워드 상품 구매에 성공했습니다."),

    GET_REWARD_REDEMPTIONS_SUCCESS(HttpStatus.OK,
            "REWARD_200_004",
            "리워드 상품 구매 내역 조회에 성공했습니다.");

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
