package com.redo.domain.user.service;

import com.redo.domain.user.converter.WithdrawalConverter;
import com.redo.domain.user.dto.WithdrawalResDTO;
import com.redo.domain.user.entity.User;
import com.redo.domain.user.entity.UserWithdrawal;
import com.redo.domain.user.entity.UserWithdrawalReason;
import com.redo.domain.user.enums.UserStatus;
import com.redo.domain.user.exception.WithdrawalErrorCode;
import com.redo.domain.user.repository.UserRepository;
import com.redo.domain.user.repository.UserWithdrawalReasonRepository;
import com.redo.domain.user.repository.UserWithdrawalRepository;
import com.redo.global.apiPayload.code.GeneralErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final UserRepository userRepository;
    private final UserWithdrawalReasonRepository userWithdrawalReasonRepository;
    private final UserWithdrawalRepository userWithdrawalRepository;
    private final StringRedisTemplate redisTemplate;

    // 탈퇴 사유 조회
    @Transactional
    public WithdrawalResDTO.ReasonList getWithdrawalReasons() {
        List<UserWithdrawalReason> reasons = userWithdrawalReasonRepository.findByIsActiveTrue();
        if (reasons.isEmpty()) {
            throw new GeneralException(WithdrawalErrorCode.WITHDRAWAL_REASON_NOT_FOUND);
        }
        return WithdrawalConverter.toReasonList(reasons);
    }
    // 회원 탈퇴 처리
    public void withdraw(Long userId, Long reasonId) {
        processWithdrawal(userId, reasonId);  // DB 작업만 트랜잭션으로

        // Redis 삭제는 트랜잭션 밖에서 (실패해도 DB 작업엔 영향 없음)
        try {
            redisTemplate.delete("refresh:" + userId);
        } catch (Exception e) {
            // Redis 삭제 실패해도, 로그만 남기고 넘어감 (탈퇴 처리 자체는 이미 성공했으니까)
        }
    }

    @Transactional
    public void processWithdrawal(Long userId, Long reasonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new GeneralException(WithdrawalErrorCode.ALREADY_WITHDRAWN_ACCOUNT);
        }

        UserWithdrawalReason reason = userWithdrawalReasonRepository.findByIdAndIsActiveTrue(reasonId)
                .orElseThrow(() -> new GeneralException(WithdrawalErrorCode.WITHDRAWAL_REASON_NOT_FOUND));

        user.withdraw();
        UserWithdrawal withdrawal = UserWithdrawal.create(user, reason);
        userWithdrawalRepository.save(withdrawal);
    }
}