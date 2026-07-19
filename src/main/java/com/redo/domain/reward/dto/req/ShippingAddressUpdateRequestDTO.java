package com.redo.domain.reward.dto.req;

import com.redo.domain.reward.enums.ShippingAddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingAddressUpdateRequestDTO(
        @NotNull(message = "배송지 유형을 선택해주세요.")
        ShippingAddressType addressType,

        @NotBlank(message = "받는 이를 입력해주세요.")
        @Size(max = 10, message = "받는 이는 10자 이하로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "받는 이는 문자만 입력해주세요.")
        String receiverName,

        @NotBlank(message = "연락처를 입력해주세요.")
        @Pattern(
                regexp = "^\\d{3}-?\\d{4}-?\\d{4}$",
                message = "연락처는 숫자 11자리 또는 000-0000-0000 형식으로 입력해주세요."
        )
        String phone,

        @NotBlank(message = "우편번호를 입력해주세요.")
        @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
        String postalCode,

        @NotBlank(message = "주소를 입력해주세요.")
        @Size(max = 255, message = "주소는 255자 이하로 입력해주세요.")
        String address1,

        @Size(max = 255, message = "상세 주소는 255자 이하로 입력해주세요.")
        String address2,

        Boolean isDefault
) {
}
