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

    REWARD_PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST,
            "REWARD_400_002",
            "판매 중인 상품이 아닙니다."),

    REWARD_PRODUCT_OUT_OF_STOCK(HttpStatus.BAD_REQUEST,
            "REWARD_400_003",
            "상품 재고가 부족합니다."),

    SHIPPING_ADDRESS_REQUIRED(HttpStatus.BAD_REQUEST,
            "REWARD_400_004",
            "배송 상품은 배송지 선택이 필요합니다."),

    COUPON_RECIPIENT_REQUIRED(HttpStatus.BAD_REQUEST,
            "REWARD_400_005",
            "기프티콘 수신자 정보를 입력해주세요."),

    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST,
            "REWARD_400_006",
            "구매 요청 멱등키가 올바르지 않습니다."),

    DUPLICATE_REDEMPTION_REQUEST(HttpStatus.CONFLICT,
            "REWARD_409_001",
            "이미 처리된 구매 요청입니다."),

    REDEMPTION_LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT,
            "REWARD_409_002",
            "다른 구매 요청을 처리 중입니다. 잠시 후 다시 시도해주세요."),

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
