package com.redo.global.ai.gemini.service;

import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiCallExecutorTest {

    private GeminiCallExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void cancelsCallAndReturnsTimeout() {
        executor = new GeminiCallExecutor(Duration.ofMillis(20));

        assertThatThrownBy(() -> executor.execute(() -> {
            Thread.sleep(1_000);
            return "late response";
        }))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.TIMEOUT);
    }
}
