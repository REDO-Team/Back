package com.redo.global.ai.gemini.exception;

import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import com.redo.global.apiPayload.exception.GeneralException;

public class GeminiException extends GeneralException {

    public GeminiException(GeminiErrorCode code) {
        super(code);
    }

    public GeminiException(GeminiErrorCode code, Throwable cause) {
        super(code);
        initCause(cause);
    }
}
