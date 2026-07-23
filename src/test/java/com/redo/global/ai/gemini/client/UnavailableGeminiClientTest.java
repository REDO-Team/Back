package com.redo.global.ai.gemini.client;

import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnavailableGeminiClientTest {

    @Test
    void generateReturnsClientDisabled() {
        GeminiClient client = new UnavailableGeminiClient();

        assertThatThrownBy(() -> client.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.CLIENT_DISABLED);
    }
}
