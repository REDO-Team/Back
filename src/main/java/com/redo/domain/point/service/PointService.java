package com.redo.domain.point.service;

import com.redo.domain.point.converter.PointConverter;
import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionResponseDTO;
import com.redo.domain.point.entity.PointTransaction;
import com.redo.domain.point.enums.PointTransactionType;
import com.redo.domain.point.exception.PointException;
import com.redo.domain.point.exception.code.PointErrorCode;
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

    // 포인트 조회 로직
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

    // 포인트 거래 내역 조회 로직
    public Page<PointTransactionResponseDTO> getMyPointTransactions(Long userId, Pageable pageable) {
        User user = getUser(userId);

        return pointTransactionRepository.findPageByUserWithRewardRedemptionAndProduct(user, pageable)
                .map(PointConverter::toPointTransactionResponse);
    }

    // 포인트 적립 로직 (인증 성공 시 내부에서 호출)
    @Transactional
    public void earnPoint(Long userId, Long certificationId, Integer amount, String idempotencyKey) {
        validateEarnPoint(amount, idempotencyKey);

        User user = getUserForUpdate(userId);

        if (pointTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        validatePointLimit(user, amount);

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .certificationId(certificationId)
                .transactionType(PointTransactionType.EARN)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .build();

        pointTransactionRepository.save(transaction);
        user.addPoint(amount);
    }

    private void validatePointLimit(User user, Integer amount) {
        if (user.getTotalPoints() > Integer.MAX_VALUE - amount) {
            throw new PointException(PointErrorCode.POINT_LIMIT_EXCEEDED);
        }
    }

    private void validateEarnPoint(Integer amount, String idempotencyKey) {
        if (amount == null || amount <= 0) {
            throw new PointException(PointErrorCode.INVALID_POINT_AMOUNT);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PointException(PointErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private User getUserForUpdate(Long userId) {
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }
}
