package com.redo.domain.contribution.exception;

import com.redo.domain.contribution.exception.code.ContributionErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class ContributionException extends GeneralException {

    public ContributionException(ContributionErrorCode code) {
        super(code);
    }
}
