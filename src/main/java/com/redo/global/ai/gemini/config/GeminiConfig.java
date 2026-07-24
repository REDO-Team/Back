package com.redo.global.ai.gemini.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.global.ai.gemini.client.GeminiClient;
import com.redo.global.ai.gemini.client.UnavailableGeminiClient;
import com.redo.global.ai.gemini.service.GeminiCallExecutor;
import com.redo.global.ai.gemini.service.SpringAiGeminiClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean(destroyMethod = "shutdown")
    public GeminiCallExecutor geminiCallExecutor(GeminiProperties properties) {
        return new GeminiCallExecutor(properties.timeout());
    }

    @Bean
    @ConditionalOnMissingBean(GeminiClient.class)
    public GeminiClient geminiClient(
            GeminiProperties properties,
            ObjectProvider<ChatModel> chatModelProvider,
            ObjectMapper objectMapper,
            GeminiCallExecutor callExecutor
    ) {
        if (!properties.enabled()) {
            return new UnavailableGeminiClient();
        }

        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            return new UnavailableGeminiClient();
        }

        return new SpringAiGeminiClient(chatModel, properties, objectMapper, callExecutor);
    }
}
