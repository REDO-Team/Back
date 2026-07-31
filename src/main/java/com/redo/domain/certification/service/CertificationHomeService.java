package com.redo.domain.certification.service;

import com.redo.domain.certification.dto.res.CertificationHomeResponseDTO;
import com.redo.domain.certification.enums.CertificationRestrictionType;
import com.redo.domain.certification.enums.CertificationSource;
import com.redo.domain.certification.service.policy.CertificationPolicyEvaluator;
import com.redo.domain.certification.service.policy.CertificationPolicyResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.COOLDOWN_SECONDS;
import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.DAILY_LIMIT;
import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.LIVE_CAPTURE_ONLY;
import static com.redo.domain.certification.service.policy.CertificationPolicyEvaluator.SAME_GUIDE_DAILY_LIMIT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificationHomeService {

    private final CertificationPolicyEvaluator certificationPolicyEvaluator;

    public CertificationHomeResponseDTO getHome(Long userId) {
        CertificationPolicyResult policy = certificationPolicyEvaluator.evaluate(userId);
        int remainingCount = (int) Math.max(0L, DAILY_LIMIT - policy.usedCount());
        CertificationHomeResponseDTO.RestrictionDTO restriction = toRestriction(policy);

        return new CertificationHomeResponseDTO(
                DAILY_LIMIT,
                remainingCount,
                policy.usedCount(),
                restriction.type() == CertificationRestrictionType.NONE,
                restriction,
                new CertificationHomeResponseDTO.PolicyDTO(
                        COOLDOWN_SECONDS,
                        SAME_GUIDE_DAILY_LIMIT,
                        LIVE_CAPTURE_ONLY
                ),
                new CertificationHomeResponseDTO.RewardPolicyDTO(
                        CertificationSource.GENERAL.rewardPoint(),
                        CertificationSource.AFTER_SEARCH.rewardPoint()
                )
        );
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
