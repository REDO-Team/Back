package com.redo.domain.contribution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redo.domain.contribution.dto.cache.ContributionEventCacheDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionFeedCacheService {

    private static final String EVENT_KEY_PREFIX = "contribution:event:";
    private static final String PARTICIPANT_COUNT_KEY = "contribution:active-participant-count";
    private static final Duration EVENT_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration PARTICIPANT_COUNT_TTL = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Map<Long, ContributionEventCacheDTO> getFeedEvents(
            Collection<Long> eventIds
    ) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<String> keys = eventIds.stream()
                    .map(this::createEventKey)
                    .toList();
            List<String> cachedValues = redisTemplate.opsForValue().multiGet(keys);
            if (cachedValues == null) {
                return Map.of();
            }

            Map<Long, ContributionEventCacheDTO> cachedEvents = new HashMap<>();
            for (String cachedValue : cachedValues) {
                ContributionEventCacheDTO cachedEvent = deserialize(cachedValue);
                if (cachedEvent != null) {
                    cachedEvents.put(cachedEvent.eventId(), cachedEvent);
                }
            }
            return cachedEvents;
        } catch (RuntimeException exception) {
            log.warn("Failed to read contribution event cache", exception);
            return Map.of();
        }
    }

    public void putFeedEvents(Collection<ContributionEventCacheDTO> events) {
        for (ContributionEventCacheDTO event : events) {
            try {
                redisTemplate.opsForValue().set(
                        createEventKey(event.eventId()),
                        objectMapper.writeValueAsString(event),
                        EVENT_CACHE_TTL
                );
            } catch (JsonProcessingException | RuntimeException exception) {
                log.warn(
                        "Failed to cache contribution event. eventId={}",
                        event.eventId(),
                        exception
                );
            }
        }
    }

    private ContributionEventCacheDTO deserialize(String cachedValue) {
        if (cachedValue == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    cachedValue,
                    ContributionEventCacheDTO.class
            );
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize contribution event cache", exception);
            return null;
        }
    }

    private String createEventKey(Long eventId) {
        return EVENT_KEY_PREFIX + eventId;
    }

    public long getActiveParticipantCount(LongSupplier countLoader) {
        try {
            String cachedCount = redisTemplate.opsForValue().get(PARTICIPANT_COUNT_KEY);
            if (cachedCount != null) {
                return Long.parseLong(cachedCount);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to read active participant count cache", exception);
        }

        long count = countLoader.getAsLong();
        try {
            redisTemplate.opsForValue().set(
                    PARTICIPANT_COUNT_KEY,
                    Long.toString(count),
                    PARTICIPANT_COUNT_TTL
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to cache active participant count", exception);
        }
        return count;
    }
}
