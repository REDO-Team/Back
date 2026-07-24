package com.redo.global.ai.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.config.GeminiProperties;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiGeminiClientTest {

    @Mock
    private ChatModel chatModel;

    private GeminiCallExecutor callExecutor;
    private SpringAiGeminiClient client;

    @BeforeEach
    void setUp() {
        callExecutor = new GeminiCallExecutor(Duration.ofSeconds(1));
        client = new SpringAiGeminiClient(
                chatModel,
                properties("test-api-key"),
                new ObjectMapper(),
                callExecutor
        );
    }

    @AfterEach
    void tearDown() {
        callExecutor.shutdown();
    }

    @Test
    void mapsMultimodalJsonRequestAndResponse() {
        ChatResponse chatResponse = response(
                """
                        {"result":"PASS","reason":"깨끗하게 분리배출되었습니다."}
                        """,
                "gemini-3.1-flash-lite",
                100,
                20,
                120,
                "STOP"
        );
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        GeminiRequest request = new GeminiRequest(
                "분리배출 검수 시스템",
                "사진을 검수해 줘",
                List.of(new GeminiMedia("image/png", new byte[]{1, 2, 3})),
                true,
                """
                        {
                          "type": "object",
                          "properties": {
                            "result": {"type": "string"},
                            "reason": {"type": "string"}
                          },
                          "required": ["result", "reason"]
                        }
                        """,
                null,
                0.1,
                512
        );

        GeminiResponse response = client.generate(request);

        assertThat(response.content()).contains("\"result\":\"PASS\"");
        assertThat(response.model()).isEqualTo("gemini-3.1-flash-lite");
        assertThat(response.promptTokens()).isEqualTo(100);
        assertThat(response.generationTokens()).isEqualTo(20);
        assertThat(response.totalTokens()).isEqualTo(120);
        assertThat(response.finishReason()).isEqualTo("STOP");

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        assertThat(prompt.getInstructions()).hasSize(2);
        assertThat(prompt.getInstructions().get(1)).isInstanceOf(UserMessage.class);
        UserMessage userMessage = (UserMessage) prompt.getInstructions().get(1);
        assertThat(userMessage.getMedia()).hasSize(1);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) prompt.getOptions();
        assertThat(options.getModel()).isEqualTo("gemini-3.1-flash-lite");
        assertThat(options.getTemperature()).isEqualTo(0.1);
        assertThat(options.getMaxOutputTokens()).isEqualTo(512);
        assertThat(options.getResponseMimeType()).isEqualTo("application/json");
        assertThat(options.getResponseSchema()).isEqualTo(request.responseSchema());
    }

    @Test
    void rejectsMissingApiKeyBeforeProviderCall() {
        SpringAiGeminiClient noKeyClient = new SpringAiGeminiClient(
                chatModel,
                properties(""),
                new ObjectMapper(),
                callExecutor
        );

        assertThatThrownBy(() -> noKeyClient.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.AUTHENTICATION_FAILED);
    }

    @Test
    void rejectsEmptyResponse() {
        ChatResponse emptyResponse = responseWithContent("");
        when(chatModel.call(any(Prompt.class))).thenReturn(emptyResponse);

        assertThatThrownBy(() -> client.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE);
    }

    @Test
    void rejectsMalformedJsonResponse() {
        ChatResponse malformedResponse = responseWithContent("not-json");
        when(chatModel.call(any(Prompt.class))).thenReturn(malformedResponse);
        GeminiRequest request = new GeminiRequest(
                null,
                "JSON으로 답해 줘",
                List.of(),
                true,
                """
                        {"type":"object"}
                        """,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> client.generate(request))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE);
    }

    @Test
    void mapsProviderServerError() {
        when(chatModel.call(any(Prompt.class))).thenThrow(
                HttpServerErrorException.create(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "provider unavailable",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        );

        assertThatThrownBy(() -> client.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.PROVIDER_ERROR);
    }

    @Test
    void mapsProviderRateLimit() {
        when(chatModel.call(any(Prompt.class))).thenThrow(
                HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "rate limited",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        );

        assertThatThrownBy(() -> client.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.RATE_LIMITED);
    }

    @Test
    void mapsProviderAuthenticationFailure() {
        when(chatModel.call(any(Prompt.class))).thenThrow(
                HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "invalid api key",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                )
        );

        assertThatThrownBy(() -> client.generate(GeminiRequest.text("분리배출 상태를 확인해 줘")))
                .isInstanceOf(GeminiException.class)
                .extracting("errorCode")
                .isEqualTo(GeminiErrorCode.AUTHENTICATION_FAILED);
    }

    private ChatResponse response(
            String content,
            String model,
            int promptTokens,
            int generationTokens,
            int totalTokens,
            String finishReason
    ) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage output =
                mock(org.springframework.ai.chat.messages.AssistantMessage.class);
        ChatGenerationMetadata generationMetadata = mock(ChatGenerationMetadata.class);
        ChatResponseMetadata responseMetadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);

        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn(content);
        when(generation.getMetadata()).thenReturn(generationMetadata);
        when(generationMetadata.getFinishReason()).thenReturn(finishReason);
        when(response.getMetadata()).thenReturn(responseMetadata);
        when(responseMetadata.getModel()).thenReturn(model);
        when(responseMetadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(promptTokens);
        when(usage.getCompletionTokens()).thenReturn(generationTokens);
        when(usage.getTotalTokens()).thenReturn(totalTokens);
        return response;
    }

    private ChatResponse responseWithContent(String content) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage output =
                mock(org.springframework.ai.chat.messages.AssistantMessage.class);

        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(output);
        when(output.getText()).thenReturn(content);
        return response;
    }

    private GeminiProperties properties(String apiKey) {
        return new GeminiProperties(
                true,
                apiKey,
                "gemini-3.1-flash-lite",
                Duration.ofSeconds(1),
                2048,
                4,
                DataSize.ofMegabytes(10)
        );
    }
}
