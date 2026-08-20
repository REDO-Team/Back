package com.redo.domain.certification.service;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.service.policy.CertificationPolicyEvaluator;
import com.redo.domain.certification.service.policy.CertificationPolicyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.LIVE_CAPTURE_ONLY;
import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.SAME_GUIDE_DAILY_LIMIT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificationHomeService {

    private static final int FRONTEND_COMPATIBLE_DAILY_LIMIT = 100;

    private final CertificationPolicyEvaluator certificationPolicyEvaluator;

    public CertificationHomeResponseDTO getHome(Long userId) {
        CertificationPolicyResult policy = certificationPolicyEvaluator.evaluate(userId);
        // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
        // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
        // int remainingCount = (int) Math.max(0L, DAILY_LIMIT - policy.usedCount());
        int remainingCount = frontendCompatibleRemainingCount(policy.usedCount());
        CertificationHomeResponseDTO.RestrictionDTO restriction = toRestriction(policy);

        return new CertificationHomeResponseDTO(
                FRONTEND_COMPATIBLE_DAILY_LIMIT,
                remainingCount,
                policy.usedCount(),
                restriction.type() == CertificationRestrictionType.NONE,
                restriction,
                new CertificationHomeResponseDTO.PolicyDTO(
                        0,
                        SAME_GUIDE_DAILY_LIMIT,
                        LIVE_CAPTURE_ONLY
                ),
                new CertificationHomeResponseDTO.RewardPolicyDTO(
                        CertificationSource.GENERAL.rewardPoint(),
                        CertificationSource.AFTER_SEARCH.rewardPoint()
                )
        );
    }

    private int frontendCompatibleRemainingCount(long usedCount) {
        long nonNegativeUsedCount = Math.max(0L, usedCount);
        long remainingCount = (long) FRONTEND_COMPATIBLE_DAILY_LIMIT - nonNegativeUsedCount;
        return (int) Math.max(1L, remainingCount);
    }

    private CertificationHomeResponseDTO.RestrictionDTO toRestriction(
            CertificationPolicyResult policy
    ) {
        return new CertificationHomeResponseDTO.RestrictionDTO(
                policy.type(),
                policy.retryAvailableAt(),
                policy.remainingSeconds(),
                policy.processingCertificationId(),
                policy.statusPath()
        );
    }
}
