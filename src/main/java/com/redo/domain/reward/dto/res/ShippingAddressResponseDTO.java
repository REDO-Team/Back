package com.redo.domain.reward.dto.res;

import com.redo.domain.reward.enums.ShippingAddressType;

public record ShippingAddressResponseDTO(
        Long shippingAddressId,
        ShippingAddressType addressType,
        String receiverName,
        String phone,
        String postalCode,
        String address1,
        String address2,
        Boolean isDefault
) {
}
