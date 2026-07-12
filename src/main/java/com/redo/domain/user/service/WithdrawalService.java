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
    // 탈퇴 처리
    @Transactional
    public void withdraw(Long userId, Long reasonId) {

        // 1) User 조회 ( 기본제공 메서드 findById 사용)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        // 2) 이미 탈퇴한 계정인지 확인
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new GeneralException(WithdrawalErrorCode.ALREADY_WITHDRAWN_ACCOUNT);
        }

        // 3) 탈퇴 사유 조회
        UserWithdrawalReason reason = userWithdrawalReasonRepository.findById(reasonId)
                .orElseThrow(() -> new GeneralException(WithdrawalErrorCode.WITHDRAWAL_REASON_NOT_FOUND));

        // 4) User 상태 변경
        user.withdraw();

        // 5) 탈퇴 이력 생성 + 저장
        UserWithdrawal withdrawal = UserWithdrawal.create(user, reason);
        userWithdrawalRepository.save(withdrawal);

        // 6) Redis에서 refreshToken 삭제
        redisTemplate.delete("refresh:" + userId);
    }
}