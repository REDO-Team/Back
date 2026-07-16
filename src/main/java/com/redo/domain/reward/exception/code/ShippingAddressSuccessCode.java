package com.redo.domain.reward.exception.code;

import com.redo.global.apiPayload.code.BaseSuccessCode;
import com.redo.global.apiPayload.code.SuccessReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShippingAddressSuccessCode implements BaseSuccessCode {

    SEARCH_SHIPPING_ADDRESS_SUCCESS(HttpStatus.OK,
            "SHIPPING_ADDRESS_200_001",
            "배송지 주소 검색에 성공했습니다."),

    GET_SHIPPING_ADDRESSES_SUCCESS(HttpStatus.OK,
            "SHIPPING_ADDRESS_200_002",
            "배송지 목록 조회에 성공했습니다."),

    CREATE_SHIPPING_ADDRESS_SUCCESS(HttpStatus.OK,
            "SHIPPING_ADDRESS_200_003",
            "배송지 생성에 성공했습니다."),

    UPDATE_SHIPPING_ADDRESS_SUCCESS(HttpStatus.OK,
            "SHIPPING_ADDRESS_200_004",
            "배송지 수정에 성공했습니다."),

    DELETE_SHIPPING_ADDRESS_SUCCESS(HttpStatus.OK,
            "SHIPPING_ADDRESS_200_005",
            "배송지 삭제에 성공했습니다.");

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
