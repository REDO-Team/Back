package com.redo.global.ai.gemini.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.client.UnavailableGeminiClient;
import com.redo.global.ai.gemini.service.SpringAiGeminiClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GeminiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeminiConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues(
                    "app.ai.gemini.api-key=",
                    "app.ai.gemini.model=gemini-3.1-flash-lite",
                    "app.ai.gemini.timeout=1s",
                    "app.ai.gemini.max-output-tokens=2048",
                    "app.ai.gemini.max-media-count=4",
                    "app.ai.gemini.max-media-size=10MB"
            );

    @Test
    void disabledEnvironmentProvidesUnavailableClientWithoutChatModel() {
        contextRunner
                .withPropertyValues("app.ai.gemini.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(GeminiClient.class);
                    assertThat(context.getBean(GeminiClient.class))
                            .isInstanceOf(UnavailableGeminiClient.class);
                });
    }

    @Test
    void enabledEnvironmentProvidesSpringAiAdapter() {
        contextRunner
                .withBean(ChatModel.class, () -> mock(ChatModel.class))
                .withPropertyValues(
                        "app.ai.gemini.enabled=true",
                        "app.ai.gemini.api-key=test-api-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(GeminiClient.class);
                    assertThat(context.getBean(GeminiClient.class))
                            .isInstanceOf(SpringAiGeminiClient.class);
                });
    }

    @Test
    void rejectsZeroTimeoutAtStartup() {
        contextRunner
                .withPropertyValues("app.ai.gemini.timeout=0s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsZeroMaxMediaSizeAtStartup() {
        contextRunner
                .withPropertyValues("app.ai.gemini.max-media-size=0B")
                .run(context -> assertThat(context).hasFailed());
    }
}
