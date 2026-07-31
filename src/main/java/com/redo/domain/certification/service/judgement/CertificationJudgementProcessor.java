package com.redo.domain.certification.service.judgement;

import com.redo.domain.certification.dto.CertificationJudgementCommand;
import com.redo.domain.certification.dto.res.CertificationCreateResponseDTO;

public interface CertificationJudgementProcessor {

    CertificationCreateResponseDTO process(CertificationJudgementCommand command);
}
