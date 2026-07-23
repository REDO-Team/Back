package com.redo.domain.certification.dto.res;

import com.redo.domain.certification.enums.CertificationRestrictionType;

import java.time.LocalDateTime;

public record CertificationHomeResponseDTO(
        int dailyLimit,
        int remainingCount,
        long usedCount,
        boolean canCertify,
        RestrictionDTO restriction,
        PolicyDTO policy,
        RewardPolicyDTO rewardPolicy
) {

    public record RestrictionDTO(
            CertificationRestrictionType type,
            LocalDateTime retryAvailableAt,
            long remainingSeconds,
            Long processingCertificationId,
            String statusPath
    ) {
    }

    public record PolicyDTO(
            long cooldownSeconds,
            int sameGuideDailyLimit,
            boolean liveCaptureOnly
    ) {
    }

    public record RewardPolicyDTO(
            int generalCertificationPoint,
            int afterSearchCertificationPoint
    ) {
    }
}
