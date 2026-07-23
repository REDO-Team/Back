package com.redo.global.ai.gemini.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.ai.gemini")
public record GeminiProperties(
        boolean enabled,
        String apiKey,
        @NotBlank String model,
        @NotNull Duration timeout,
        @Min(1) @Max(10) int maxAttempts,
        @Min(1) int maxOutputTokens,
        @Min(1) int maxMediaCount,
        @NotNull DataSize maxMediaSize
) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
