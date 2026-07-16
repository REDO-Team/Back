package com.redo.domain.reward.dto.res;

import java.util.List;

public record ShippingAddressSearchResponseDTO(
        List<ShippingAddressCandidateResponseDTO> addressCandidates,
        int page,
        int size,
        int totalCount,
        boolean hasNext
) {
}
