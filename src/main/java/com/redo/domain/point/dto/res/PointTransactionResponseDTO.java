package com.redo.domain.point.dto.res;

import com.redo.domain.point.enums.PointTransactionType;

import java.time.LocalDateTime;

public record PointTransactionResponseDTO(
        Long transactionId,
        String title,
        PointTransactionType transactionType,
        Integer amount,
        Long certificationId,
        Long rewardRedemptionId,
        LocalDateTime createdAt
) {
}
