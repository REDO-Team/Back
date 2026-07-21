package com.redo.domain.point.converter;

import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionPageResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionResponseDTO;
import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.user.entity.User;
import org.springframework.data.domain.Page;

public class PointConverter {

    private PointConverter() {
    }

    public static PointBalanceResponseDTO toPointBalanceResponse(User user, Integer monthlyEarnedPoint) {
        return new PointBalanceResponseDTO(user.getTotalPoints(), monthlyEarnedPoint);
    }

    public static PointTransactionResponseDTO toPointTransactionResponse(PointTransaction transaction) {
        return new PointTransactionResponseDTO(
                transaction.getId(),
                resolveTitle(transaction),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCertificationId(),
                transaction.getRewardRedemption() == null ? null : transaction.getRewardRedemption().getId(),
                transaction.getCreatedAt()
        );
    }

    private static String resolveTitle(PointTransaction transaction) {
        if (transaction.getTransactionType() == PointTransactionType.EARN) {
            return "분리수거 인증";
        }

        if (transaction.getTransactionType() == PointTransactionType.USE
                && transaction.getRewardRedemption() != null) {
            return transaction.getRewardRedemption().getProductName() + " 구매";
        }

        return "포인트 내역";
    }

    public static PointTransactionPageResponseDTO toPointTransactionPageResponse(
            Page<PointTransactionResponseDTO> page
    ) {
        return new PointTransactionPageResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}
