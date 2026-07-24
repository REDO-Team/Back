package com.redo.global.ai.gemini.client;

import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;

public interface GeminiClient {

    GeminiResponse generate(GeminiRequest request);
}
