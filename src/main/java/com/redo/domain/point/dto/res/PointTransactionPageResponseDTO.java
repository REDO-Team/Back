package com.redo.domain.point.dto.res;

import java.util.List;

public record PointTransactionPageResponseDTO(
        List<PointTransactionResponseDTO> items,
        int page,
        int size,
        boolean hasNext
) {
}
