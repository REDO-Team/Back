package com.redo.domain.reward.converter;

import com.redo.domain.reward.dto.req.ShippingAddressCreateRequestDTO;
import com.redo.domain.reward.dto.res.ShippingAddressDeleteResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressListResponseDTO;
import com.redo.domain.reward.dto.res.ShippingAddressResponseDTO;
import com.redo.domain.reward.entity.ShippingAddress;
import com.redo.domain.user.entity.User;

import java.util.List;

public class ShippingAddressConverter {

    private ShippingAddressConverter() {
    }

    public static ShippingAddress toShippingAddress(
            ShippingAddressCreateRequestDTO request,
            User user,
            boolean isDefault
    ) {
        return ShippingAddress.builder()
                .user(user)
                .addressType(request.addressType())
                .receiverName(request.receiverName())
                .phone(request.phone())
                .postalCode(request.postalCode())
                .address1(request.address1())
                .address2(request.address2())
                .isDefault(isDefault)
                .build();
    }

    public static ShippingAddressResponseDTO toShippingAddressResponse(ShippingAddress shippingAddress) {
        return new ShippingAddressResponseDTO(
                shippingAddress.getId(),
                shippingAddress.getAddressType(),
                shippingAddress.getReceiverName(),
                shippingAddress.getPhone(),
                shippingAddress.getPostalCode(),
                shippingAddress.getAddress1(),
                shippingAddress.getAddress2(),
                shippingAddress.getIsDefault()
        );
    }

    public static ShippingAddressListResponseDTO toShippingAddressListResponse(
            List<ShippingAddress> shippingAddresses
    ) {
        return new ShippingAddressListResponseDTO(
                shippingAddresses.stream()
                        .map(ShippingAddressConverter::toShippingAddressResponse)
                        .toList()
        );
    }

    public static ShippingAddressDeleteResponseDTO toShippingAddressDeleteResponse(Long shippingAddressId) {
        return new ShippingAddressDeleteResponseDTO(shippingAddressId);
    }
}
