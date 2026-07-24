package com.redo.global.ai.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.config.GeminiProperties;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiRequestValidatorTest {

    private GeminiRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GeminiRequestValidator(properties(), new ObjectMapper());
    }

    @Test
    void acceptsTextAndImageJsonRequest() {
        GeminiRequest request = new GeminiRequest(
                "분리배출 검수 시스템",
                "사진을 검수해 줘",
                List.of(new GeminiMedia("image/png", new byte[]{1, 2, 3})),
                true,
                """
                        {
                          "type": "object",
                          "properties": {
                            "result": {"type": "string"}
                          },
                          "required": ["result"]
                        }
                        """,
                null,
                0.1,
                512
        );

        assertThatCode(() -> validator.validate(request))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankPrompt() {
        assertError(
                GeminiRequest.text(" "),
                GeminiErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void rejectsEmptyMedia() {
        GeminiRequest request = new GeminiRequest(
                null,
                "사진을 검수해 줘",
                List.of(new GeminiMedia("image/png", new byte[0])),
                false,
                null,
                null,
                null,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_REQUEST);
    }

    @Test
    void rejectsNonImageMedia() {
        GeminiRequest request = new GeminiRequest(
                null,
                "파일을 검수해 줘",
                List.of(new GeminiMedia("application/pdf", new byte[]{1})),
                false,
                null,
                null,
                null,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_REQUEST);
    }

    @Test
    void rejectsInvalidTemperature() {
        GeminiRequest request = new GeminiRequest(
                null,
                "사진을 검수해 줘",
                List.of(),
                false,
                null,
                null,
                1.1,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_REQUEST);
    }

    @Test
    void rejectsNonFiniteTemperature() {
        GeminiRequest request = new GeminiRequest(
                null,
                "사진을 검수해 줘",
                List.of(),
                false,
                null,
                null,
                Double.NaN,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_REQUEST);
    }

    @Test
    void requiresSchemaInJsonMode() {
        GeminiRequest request = new GeminiRequest(
                null,
                "JSON으로 답해 줘",
                List.of(),
                true,
                null,
                null,
                null,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
    }

    @Test
    void rejectsMalformedJsonSchema() {
        GeminiRequest request = new GeminiRequest(
                null,
                "JSON으로 답해 줘",
                List.of(),
                true,
                "{not-json}",
                null,
                null,
                null
        );

        assertError(request, GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
    }

    @Test
    void rejectsScalarJsonSchema() {
        assertError(jsonRequest("\"text\""), GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
    }

    @Test
    void rejectsJsonSchemaWithoutType() {
        assertError(jsonRequest("{\"properties\":{}}"), GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
    }

    @Test
    void rejectsJsonSchemaWithUnsupportedType() {
        assertError(jsonRequest("{\"type\":\"null\"}"), GeminiErrorCode.INVALID_RESPONSE_SCHEMA);
    }

    @Test
    void acceptsArrayOutputSchemaDocument() {
        assertThatCode(() -> validator.validate(jsonRequest(
                "{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"
        ))).doesNotThrowAnyException();
    }

    private void assertError(GeminiRequest request, GeminiErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private GeminiRequest jsonRequest(String responseSchema) {
        return new GeminiRequest(
                null,
                "JSON으로 답해 줘",
                List.of(),
                true,
                responseSchema,
                null,
                null,
                null
        );
    }

    private GeminiProperties properties() {
        return new GeminiProperties(
                true,
                "test-api-key",
                "gemini-3.1-flash-lite",
                Duration.ofSeconds(1),
                2048,
                4,
                DataSize.ofMegabytes(10)
        );
    }
}
