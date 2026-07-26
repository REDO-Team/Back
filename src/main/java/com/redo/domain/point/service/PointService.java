package com.redo.domain.point.service;

import com.redo.domain.certification.entity.Certification;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.repository.CertificationRepository;
import com.redo.domain.point.converter.PointConverter;
import com.redo.domain.point.dto.res.PointBalanceResponseDTO;
import com.redo.domain.point.dto.res.PointTransactionPageResponseDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private static final int DAILY_EARN_LIMIT = 3;
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final PointTransactionRepository pointTransactionRepository;
    private final CertificationRepository certificationRepository;
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
    public PointTransactionPageResponseDTO getMyPointTransactions(
            Long userId,
            Long cursor,
            int size
    ) {
        User user = getUser(userId);
        List<PointTransaction> transactions =
                pointTransactionRepository.findByUserWithRewardRedemptionAndProduct(
                        user,
                        cursor,
                        PageRequest.of(0, size + 1)
                );
        boolean hasNext = transactions.size() > size;
        List<PointTransaction> pageTransactions = hasNext
                ? transactions.subList(0, size)
                : transactions;
        List<PointTransactionResponseDTO> items = pageTransactions.stream()
                .map(PointConverter::toPointTransactionResponse)
                .toList();
        Long nextCursor = hasNext && !pageTransactions.isEmpty()
                ? pageTransactions.get(pageTransactions.size() - 1).getId()
                : null;

        return PointConverter.toPointTransactionPageResponse(
                items,
                nextCursor,
                hasNext
        );
    }

    // 포인트 적립 로직 (인증 성공 시 내부에서 호출)
    @Transactional
    public void earnPoint(
            Long userId,
            Long certificationId,
            CertificationSource certificationSource,
            String idempotencyKey
    ) {
        validateEarnPoint(certificationSource, idempotencyKey);

        User user = getUserForUpdate(userId);

        if (pointTransactionRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }

        Certification certification = getCertification(certificationId, userId);
        validateCertificationSource(certification, certificationSource);
        int amount = certification.getRewardPoint();

        validateDailyEarnLimit(user);
        validatePointLimit(user, amount);

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .certification(certification)
                .transactionType(PointTransactionType.EARN)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .build();

        pointTransactionRepository.save(transaction);
        user.addPoint(amount);
    }

    private void validateDailyEarnLimit(User user) {
        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();

        long dailyEarnCount = pointTransactionRepository
                .countByUserAndTransactionTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        user,
                        PointTransactionType.EARN,
                        startAt,
                        endAt
                );

        if (dailyEarnCount >= DAILY_EARN_LIMIT) {
            throw new PointException(PointErrorCode.DAILY_EARN_LIMIT_EXCEEDED);
        }
    }

    private void validatePointLimit(User user, Integer amount) {
        if (user.getTotalPoints() > Integer.MAX_VALUE - amount) {
            throw new PointException(PointErrorCode.POINT_LIMIT_EXCEEDED);
        }
    }

    private void validateEarnPoint(CertificationSource certificationSource, String idempotencyKey) {
        if (certificationSource == null) {
            throw new PointException(PointErrorCode.INVALID_CERTIFICATION_SOURCE);
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PointException(PointErrorCode.INVALID_IDEMPOTENCY_KEY);
        }
    }

    private void validateCertificationSource(
            Certification certification,
            CertificationSource certificationSource
    ) {
        if (certification.getCertificationSource() != certificationSource) {
            throw new PointException(PointErrorCode.INVALID_CERTIFICATION_SOURCE);
        }
    }

    private Certification getCertification(Long certificationId, Long userId) {
        return certificationRepository.findByIdAndUserId(certificationId, userId)
                .orElseThrow(() -> new PointException(PointErrorCode.CERTIFICATION_NOT_FOUND));
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
