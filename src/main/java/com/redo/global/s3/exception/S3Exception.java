package com.redo.global.s3.exception;

import com.redo.global.apiPayload.exception.GeneralException;
import com.redo.global.s3.exception.code.S3ErrorCode;

public class S3Exception extends GeneralException {

    public S3Exception(S3ErrorCode code) {
        super(code);
    }
}
