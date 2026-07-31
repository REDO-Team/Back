package com.redo.domain.certification.exception;

import com.redo.domain.certification.exception.code.CertificationErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;
import lombok.Getter;

@Getter
public class CertificationException extends GeneralException {

    private final Object errorDetail;

    public CertificationException(
            CertificationErrorCode errorCode,
            Object errorDetail
    ) {
        super(errorCode);
        this.errorDetail = errorDetail;
    }
}
