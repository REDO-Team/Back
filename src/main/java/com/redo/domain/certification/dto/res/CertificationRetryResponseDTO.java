package com.redo.domain.certification.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationRetryResponseDTO(
        Long certificationId,
        CertificationStatus status,
        CertificationFailureType failureType,
        int attemptCount,
        Long recycleGuideId,
        String itemName,
        String categoryName,
        int earnedPoint,
        String failedReason,
        List<String> retryGuide,
        boolean retryAllowed,
        String retryPath,
        LocalDateTime judgedAt
) {

    public CertificationRetryResponseDTO {
        retryGuide = retryGuide == null ? List.of() : List.copyOf(retryGuide);
    }

    public static CertificationRetryResponseDTO from(
            CertificationCreateResponseDTO response,
            int attemptCount
    ) {
        return new CertificationRetryResponseDTO(
                response.certificationId(),
                response.status(),
                response.failureType(),
                attemptCount,
                response.recycleGuideId(),
                response.itemName(),
                response.categoryName(),
                response.earnedPoint(),
                response.failedReason(),
                response.retryGuide(),
                response.retryAllowed(),
                response.retryPath(),
                response.judgedAt()
        );
    }
}
