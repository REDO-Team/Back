package com.redo.domain.community.exception;

import com.redo.domain.community.exception.code.CommunityErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class CommunityException extends GeneralException {
    public CommunityException(CommunityErrorCode code) {
        super(code);
    }
}
