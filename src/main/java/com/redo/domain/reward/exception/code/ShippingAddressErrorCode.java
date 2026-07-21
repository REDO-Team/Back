package com.redo.domain.reward.exception.code;

import com.redo.global.apiPayload.code.BaseErrorCode;
import com.redo.global.apiPayload.code.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShippingAddressErrorCode implements BaseErrorCode {

    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST,
            "SHIPPING_ADDRESS_400_001",
            "페이지 요청 값이 올바르지 않습니다."),

    INVALID_SHIPPING_ADDRESS_REQUEST(HttpStatus.BAD_REQUEST,
            "SHIPPING_ADDRESS_400_002",
            "배송지 요청 값이 올바르지 않습니다."),

    SHIPPING_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "SHIPPING_ADDRESS_404_001",
            "배송지를 찾을 수 없습니다."),

    SHIPPING_ADDRESS_FORBIDDEN(HttpStatus.FORBIDDEN,
            "SHIPPING_ADDRESS_403_001",
            "해당 배송지에 접근할 수 없습니다."),

    ADDRESS_SEARCH_FAILED(HttpStatus.BAD_GATEWAY,
            "SHIPPING_ADDRESS_502_001",
            "주소 검색에 실패했습니다.");

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
