package com.redo.domain.certification.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redo.domain.certification.enums.CertificationRestrictionType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationErrorDetail(
        String type,
        Long certificationId,
        String statusPath,
        Long recycleGuideId
) {

    // 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
    // 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
    // 기존 record 필드:
    // Integer dailyLimit,
    // Long usedCount,
    // LocalDateTime retryAvailableAt,
    // Long remainingSeconds,

    public static CertificationErrorDetail type(String type) {
        return new CertificationErrorDetail(
                type, null, null, null
        );
    }

    public static CertificationErrorDetail guide(String type, Long recycleGuideId) {
        return new CertificationErrorDetail(
                type, null, null, recycleGuideId
        );
    }

    /*
     * 데모데이 시현을 위해 일일 3회/5분 제한을 비활성화함 (2026-08-20)
     * 데모데이 종료 후 정책 복구 여부를 확인한 뒤 재활성화할 것
     *
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
    */

    public static CertificationErrorDetail processing(
            Long certificationId,
            String statusPath
    ) {
        return new CertificationErrorDetail(
                CertificationRestrictionType.PROCESSING_EXISTS.name(),
                certificationId,
                statusPath,
                null
        );
    }
}
