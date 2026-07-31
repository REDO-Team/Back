package com.redo.domain.certification.service.policy;

import com.redo.domain.certification.enums.CertificationRestrictionType;

import java.time.LocalDateTime;

public record CertificationPolicyResult(
        long usedCount,
        CertificationRestrictionType type,
        LocalDateTime retryAvailableAt,
        long remainingSeconds,
        Long processingCertificationId,
        String statusPath
) {
}
