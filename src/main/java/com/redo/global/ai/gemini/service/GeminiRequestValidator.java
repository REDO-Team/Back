package com.redo.global.ai.gemini.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.config.GeminiProperties;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import java.util.Set;

final class GeminiRequestValidator {

    private static final Set<String> SUPPORTED_SCHEMA_TYPES =
            Set.of("object", "array", "string", "number", "integer", "boolean");

    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    GeminiRequestValidator(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    void validate(GeminiRequest request) {
        if (request == null || !StringUtils.hasText(request.userPrompt())) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
        }
        if (request.media().size() > properties.maxMediaCount()) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
        }

        request.media().forEach(this::validateMedia);
        validateOptions(request);
        validateResponseSchema(request);
    }

    private void validateMedia(GeminiMedia media) {
        if (media == null || media.size() == 0 || media.size() > properties.maxMediaSize().toBytes()) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
        }

        try {
            MimeType mimeType = MimeTypeUtils.parseMimeType(media.mimeType());
            if (!"image".equalsIgnoreCase(mimeType.getType())) {
                throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
            }
        } catch (IllegalArgumentException exception) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST, exception);
        }
    }

    private void validateOptions(GeminiRequest request) {
        if (request.temperature() != null
                && (!Double.isFinite(request.temperature())
                || request.temperature() < 0.0
                || request.temperature() > 1.0)) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
        }
        if (request.maxOutputTokens() != null && request.maxOutputTokens() <= 0) {
            throw new GeminiException(GeminiErrorCode.INVALID_REQUEST);
        }
    }

    private void validateResponseSchema(GeminiRequest request) {
        if (!request.jsonResponse() && StringUtils.hasText(request.responseSchema())) {
            throw new GeminiException(GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
        }
        if (!request.jsonResponse()) {
            return;
        }
        if (!StringUtils.hasText(request.responseSchema())) {
            throw new GeminiException(GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
        }

        try {
            JsonNode schema = objectMapper.readTree(request.responseSchema());
            JsonNode type = schema == null || !schema.isObject() ? null : schema.get("type");
            if (type == null || !type.isTextual() || !SUPPORTED_SCHEMA_TYPES.contains(type.textValue())) {
                throw new GeminiException(GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
            }
        } catch (JsonProcessingException exception) {
            throw new GeminiException(GeminiErrorCode.INVALID_RESPONSE_SCHEMA, exception);
        }
    }
}
