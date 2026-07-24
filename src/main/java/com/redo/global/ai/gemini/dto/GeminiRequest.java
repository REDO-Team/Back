package com.redo.global.ai.gemini.dto;

import java.util.List;

public record GeminiRequest(
        String systemInstruction,
        String userPrompt,
        List<GeminiMedia> media,
        boolean jsonResponse,
        String responseSchema,
        String model,
        Double temperature,
        Integer maxOutputTokens
) {

    public GeminiRequest {
        media = media == null ? List.of() : List.copyOf(media);
    }

    public static GeminiRequest text(String userPrompt) {
        return new GeminiRequest(
                null,
                userPrompt,
                List.of(),
                false,
                null,
                null,
                null,
                null
        );
    }

    @Override
    public String toString() {
        return "GeminiRequest{mediaCount=%d, jsonResponse=%s, model='%s'}"
                .formatted(media.size(), jsonResponse, model);
    }
}
