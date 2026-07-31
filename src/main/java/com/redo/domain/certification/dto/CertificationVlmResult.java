package com.redo.domain.certification.dto;

import com.redo.domain.certification.enums.AiJudgementResult;

import java.util.List;

public record CertificationVlmResult(
        AiJudgementResult result,
        String reason,
        List<String> retryGuide,
        String rawResponseJson
) {

    public CertificationVlmResult {
        retryGuide = retryGuide == null ? List.of() : List.copyOf(retryGuide);
    }
}
