package com.redo.domain.certification.exception;

import com.redo.domain.certification.dto.res.CertificationErrorDetail;
import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.Getter;

@Getter
public class CertificationException extends GeneralException {

    private final CertificationErrorDetail errorDetail;

    public CertificationException(
            CertificationErrorCode errorCode,
            CertificationErrorDetail errorDetail
    ) {
        super(errorCode);
        this.errorDetail = errorDetail;
    }
}
