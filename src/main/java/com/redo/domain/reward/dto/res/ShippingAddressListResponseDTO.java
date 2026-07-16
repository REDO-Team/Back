package com.redo.domain.reward.dto.res;

import java.util.List;

public record ShippingAddressListResponseDTO(
        List<ShippingAddressResponseDTO> shippingAddresses
) {
}
