package com.redo.domain.point.exception;

import com.redo.domain.point.exception.code.PointErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class PointException extends GeneralException {
    public PointException(PointErrorCode code) {
        super(code);
    }
}
