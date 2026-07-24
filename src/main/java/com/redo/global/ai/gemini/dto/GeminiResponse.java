package com.redo.global.ai.gemini.dto;

public record GeminiResponse(
        String content,
        String model,
        Integer promptTokens,
        Integer generationTokens,
        Integer totalTokens,
        String finishReason
) {
}
