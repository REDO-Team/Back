package com.redo.domain.reward.dto.res;

public record ShippingAddressCandidateResponseDTO(
        String roadAddress,
        String jibunAddress,
        String postalCode,
        String buildingName
) {
}
