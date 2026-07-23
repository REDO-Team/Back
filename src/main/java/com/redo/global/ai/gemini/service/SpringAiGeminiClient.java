package com.redo.global.ai.gemini.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.config.GeminiProperties;
import com.redo.global.ai.gemini.dto.GeminiMedia;
import com.redo.global.ai.gemini.dto.GeminiRequest;
import com.redo.global.ai.gemini.dto.GeminiResponse;
import com.redo.global.ai.gemini.exception.GeminiException;
import com.redo.global.ai.gemini.exception.code.GeminiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
public class SpringAiGeminiClient implements GeminiClient {

    private static final String JSON_MIME_TYPE = "application/json";

    private final ChatModel chatModel;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiCallExecutor callExecutor;
    private final GeminiRequestValidator requestValidator;

    public SpringAiGeminiClient(
            ChatModel chatModel,
            GeminiProperties properties,
            ObjectMapper objectMapper,
            GeminiCallExecutor callExecutor
    ) {
        this.chatModel = chatModel;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.callExecutor = callExecutor;
        this.requestValidator = new GeminiRequestValidator(properties, objectMapper);
    }

    @Override
    public GeminiResponse generate(GeminiRequest request) {
        if (!properties.hasApiKey()) {
            throw new GeminiException(GeminiErrorCode.AUTHENTICATION_FAILED);
        }
        requestValidator.validate(request);

        String model = StringUtils.hasText(request.model()) ? request.model() : properties.model();
        long startedAt = System.nanoTime();

        try {
            ChatResponse response = callExecutor.execute(() -> chatModel.call(toPrompt(request, model)));
            GeminiResponse result = toResponse(request, response, model);
            log.info(
                    "Gemini call completed. model={}, durationMs={}, totalTokens={}",
                    result.model(),
                    elapsedMillis(startedAt),
                    result.totalTokens()
            );
            return result;
        } catch (GeminiException exception) {
            log.warn(
                    "Gemini call failed. model={}, durationMs={}, code={}",
                    model,
                    elapsedMillis(startedAt),
                    exception.getErrorCode().getCode()
            );
            throw exception;
        } catch (RuntimeException exception) {
            GeminiException mappedException = mapProviderException(exception);
            log.warn(
                    "Gemini call failed. model={}, durationMs={}, code={}",
                    model,
                    elapsedMillis(startedAt),
                    mappedException.getErrorCode().getCode()
            );
            throw mappedException;
        }
    }

    private Prompt toPrompt(GeminiRequest request, String model) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(request.systemInstruction())) {
            messages.add(new SystemMessage(request.systemInstruction()));
        }

        List<Media> media = request.media().stream()
                .map(this::toMedia)
                .toList();
        messages.add(UserMessage.builder()
                .text(request.userPrompt())
                .media(media)
                .build());

        var optionsBuilder = GoogleGenAiChatOptions.builder()
                .model(model)
                .maxOutputTokens(
                        request.maxOutputTokens() == null
                                ? properties.maxOutputTokens()
                                : request.maxOutputTokens()
                );
        if (request.temperature() != null) {
            optionsBuilder.temperature(request.temperature());
        }
        if (request.jsonResponse()) {
            optionsBuilder
                    .responseMimeType(JSON_MIME_TYPE)
                    .responseSchema(request.responseSchema());
        }

        return new Prompt(messages, optionsBuilder.build());
    }

    private Media toMedia(GeminiMedia media) {
        return new Media(
                MimeTypeUtils.parseMimeType(media.mimeType()),
                new ByteArrayResource(media.data())
        );
    }

    private GeminiResponse toResponse(GeminiRequest request, ChatResponse response, String requestedModel) {
        if (response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null
                || !StringUtils.hasText(response.getResult().getOutput().getText())) {
            throw new GeminiException(GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE);
        }

        String content = response.getResult().getOutput().getText();
        if (request.jsonResponse()) {
            validateJsonResponse(content);
        }

        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        String responseModel = response.getMetadata() == null ? null : response.getMetadata().getModel();
        String finishReason = response.getResult().getMetadata() == null
                ? null
                : response.getResult().getMetadata().getFinishReason();

        return new GeminiResponse(
                content,
                StringUtils.hasText(responseModel) ? responseModel : requestedModel,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens(),
                finishReason
        );
    }

    private void validateJsonResponse(String content) {
        try {
            objectMapper.readTree(content);
        } catch (JsonProcessingException exception) {
            throw new GeminiException(GeminiErrorCode.EMPTY_OR_INVALID_RESPONSE, exception);
        }
    }

    private GeminiException mapProviderException(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof java.net.SocketTimeoutException) {
                return new GeminiException(GeminiErrorCode.TIMEOUT, exception);
            }

            HttpStatusCode statusCode = extractStatusCode(current);
            if (statusCode != null) {
                int status = statusCode.value();
                if (status == 401 || status == 403) {
                    return new GeminiException(GeminiErrorCode.AUTHENTICATION_FAILED, exception);
                }
                if (status == 429) {
                    return new GeminiException(GeminiErrorCode.RATE_LIMITED, exception);
                }
                if (status >= 500) {
                    return new GeminiException(GeminiErrorCode.PROVIDER_ERROR, exception);
                }
                if (status >= 400) {
                    return new GeminiException(GeminiErrorCode.INVALID_REQUEST, exception);
                }
            }
            current = current.getCause();
        }
        return new GeminiException(GeminiErrorCode.PROVIDER_ERROR, exception);
    }

    private HttpStatusCode extractStatusCode(Throwable throwable) {
        if (throwable instanceof HttpStatusCodeException exception) {
            return exception.getStatusCode();
        }
        if (throwable instanceof WebClientResponseException exception) {
            return exception.getStatusCode();
        }
        return null;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
