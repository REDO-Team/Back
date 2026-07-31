package com.redo.domain.certification.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;

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
                "DAILY_LIMIT_EXCEEDED",
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
                "COOLDOWN",
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
                "PROCESSING_EXISTS",
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
