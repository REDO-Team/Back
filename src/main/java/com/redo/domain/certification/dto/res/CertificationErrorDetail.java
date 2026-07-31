package com.redo.domain.certification.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redo.domain.certification.enums.CertificationRestrictionType;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationErrorDetail(
        String type,
        Integer dailyLimit,
        Long usedCount,
        LocalDateTime retryAvailableAt,
        Long remainingSeconds,
        Long certificationId,
        String statusPath,
        Long recycleGuideId
) {

    public static CertificationErrorDetail type(String type) {
        return new CertificationErrorDetail(
                type, null, null, null, null, null, null, null
        );
    }

    public static CertificationErrorDetail guide(String type, Long recycleGuideId) {
        return new CertificationErrorDetail(
                type, null, null, null, null, null, null, recycleGuideId
        );
    }

    public static CertificationErrorDetail dailyLimit(
            int dailyLimit,
            long usedCount
    ) {
        return new CertificationErrorDetail(
                CertificationRestrictionType.DAILY_LIMIT_EXCEEDED.name(),
                dailyLimit,
                usedCount,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static CertificationErrorDetail cooldown(
            LocalDateTime retryAvailableAt,
            long remainingSeconds
    ) {
        return new CertificationErrorDetail(
                CertificationRestrictionType.COOLDOWN.name(),
                null,
                null,
                retryAvailableAt,
                remainingSeconds,
                null,
                null,
                null
        );
    }

    public static CertificationErrorDetail processing(
            Long certificationId,
            String statusPath
    ) {
        return new CertificationErrorDetail(
                CertificationRestrictionType.PROCESSING_EXISTS.name(),
                null,
                null,
                null,
                null,
                certificationId,
                statusPath,
                null
        );
    }
}
