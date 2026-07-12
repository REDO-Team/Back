package com.redo.domain.point.service;

import com.redo.domain.point.converter.PointConverter;
import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionResponseDTO;
import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.point.exception.PointErrorCode;
import com.redo.domain.point.repository.PointTransactionRepository;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.exception.UserErrorCode;
import com.redo.domain.user.repository.UserRepository;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    public PointBalanceResponseDTO getMyPoint(Long userId) {
        User user = getUser(userId);

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startAt = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        Long monthlyEarnedPoint = pointTransactionRepository
                .sumAmountByUserAndTransactionTypeAndCreatedAtBetween(
                        user,
                        PointTransactionType.EARN,
                        startAt,
                        endAt
                );

        return PointConverter.toPointBalanceResponse(user, monthlyEarnedPoint.intValue());
    }

    public Page<PointTransactionResponseDTO> getMyPointTransactions(Long userId, Pageable pageable) {
        User user = getUser(userId);

        return pointTransactionRepository.findPageByUserWithRewardRedemptionAndProduct(user, pageable)
                .map(PointConverter::toPointTransactionResponse);
    }

    // 포인트 적립 메소드
    @Transactional
    public void earnPoint(Long userId, Long certificationId, Integer amount, String idempotencyKey) {
        validateEarnPoint(amount, idempotencyKey);

        if (pointTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        User user = getUser(userId);

        user.addPoint(amount);

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .certificationId(certificationId)
                .transactionType(PointTransactionType.EARN)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .build();

        pointTransactionRepository.save(transaction);
    }

    private void validateEarnPoint(Integer amount, String idempotencyKey) {
        if (amount == null || amount <= 0) {
            throw new GeneralException(PointErrorCode.INVALID_POINT_AMOUNT);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new GeneralException(PointErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
