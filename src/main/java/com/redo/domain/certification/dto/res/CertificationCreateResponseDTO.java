package com.redo.domain.certification.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redo.domain.certification.enums.CertificationFailureType;
import com.redo.domain.certification.enums.CertificationStatus;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CertificationCreateResponseDTO(
        Long certificationId,
        CertificationStatus status,
        CertificationFailureType failureType,
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

    public CertificationCreateResponseDTO {
        retryGuide = retryGuide == null ? List.of() : List.copyOf(retryGuide);
    }
}
