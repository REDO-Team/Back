package com.redo.domain.certification.dto;

import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;

public record CertificationJudgementPreparation(
        CertificationJudgementContext context,
        CertificationCreateResponseDTO completedResponse
) {

    public static CertificationJudgementPreparation ready(
            CertificationJudgementContext context
    ) {
        return new CertificationJudgementPreparation(context, null);
    }

    public static CertificationJudgementPreparation completed(
            CertificationCreateResponseDTO response
    ) {
        return new CertificationJudgementPreparation(null, response);
    }

    public boolean isCompleted() {
        return completedResponse != null;
    }
}
