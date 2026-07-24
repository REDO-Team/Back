package com.redo.global.ai.gemini.config;

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
        @Min(1) int maxOutputTokens,
        @Min(1) int maxMediaCount,
        @NotNull DataSize maxMediaSize
) {

    public GeminiProperties {
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("Gemini timeout must be positive.");
        }
        if (maxMediaSize != null && maxMediaSize.toBytes() <= 0) {
            throw new IllegalArgumentException("Gemini max media size must be positive.");
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
