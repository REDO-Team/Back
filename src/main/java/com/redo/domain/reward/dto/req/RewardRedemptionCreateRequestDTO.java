package com.redo.domain.reward.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RewardRedemptionCreateRequestDTO(
        @NotNull(message = "리워드 상품을 선택해주세요.")
        @Positive(message = "리워드 상품 ID는 0보다 커야 합니다.")
        Long rewardProductId,

        @Positive(message = "배송지 ID는 0보다 커야 합니다.")
        Long shippingAddressId,

        @Size(max = 10, message = "받는 이는 10자 이하로 입력해주세요.")
        @Pattern(regexp = "^[가-힣a-zA-Z ]+$", message = "받는 이는 문자만 입력해주세요.")
        String receiverName,

        @Pattern(
                regexp = "^(\\d{11}|\\d{3}-\\d{4}-\\d{4})$",
                message = "연락처는 숫자 11자리 또는 000-0000-0000 형식으로 입력해주세요."
        )
        String receiverPhone
) {
}
