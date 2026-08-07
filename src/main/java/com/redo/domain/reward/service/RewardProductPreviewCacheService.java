package com.redo.domain.reward.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardProductPreviewCacheService {

    private static final String PREVIEW_KEY_PREFIX = "reward:product-preview:";
    private static final TypeReference<List<Long>> PRODUCT_ID_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<List<Long>> getProductIds(Long userId, LocalDate previewDate) {
        try {
            String cachedValue = redisTemplate.opsForValue().get(createKey(userId, previewDate));
            if (cachedValue == null) {
                return Optional.empty();
            }

            return Optional.of(List.copyOf(
                    objectMapper.readValue(cachedValue, PRODUCT_ID_LIST_TYPE)
            ));
        } catch (JsonProcessingException | RuntimeException exception) {
            log.warn(
                    "Failed to read reward product preview cache. userId={}, previewDate={}",
                    userId,
                    previewDate,
                    exception
            );
            return Optional.empty();
        }
    }

    public void putProductIds(
            Long userId,
            LocalDate previewDate,
            List<Long> productIds,
            Duration ttl
    ) {
        try {
            redisTemplate.opsForValue().set(
                    createKey(userId, previewDate),
                    objectMapper.writeValueAsString(productIds),
                    ttl
            );
        } catch (JsonProcessingException | RuntimeException exception) {
            log.warn(
                    "Failed to cache reward product preview. userId={}, previewDate={}",
                    userId,
                    previewDate,
                    exception
            );
        }
    }

    private String createKey(Long userId, LocalDate previewDate) {
        return PREVIEW_KEY_PREFIX + previewDate + ":" + userId;
    }
}
