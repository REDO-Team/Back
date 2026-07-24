package com.redo.global.ai.gemini.client;

import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;

public class UnavailableGeminiClient implements GeminiClient {

    @Override
    public GeminiResponse generate(GeminiRequest request) {
        throw new GeminiException(GeminiErrorCode.CLIENT_DISABLED);
    }
}
