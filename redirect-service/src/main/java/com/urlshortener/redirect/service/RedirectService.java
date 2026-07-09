package com.urlshortener.redirect.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.urlshortener.common.dto.ClickEventMessage;
import com.urlshortener.redirect.config.AppConfig;
import com.urlshortener.redirect.dto.CachedUrl;
import com.urlshortener.redirect.entity.UrlMapping;
import com.urlshortener.redirect.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private static final String CACHE_PREFIX = "url:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;
    private final UrlMappingRepository urlMappingRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public Optional<CachedUrl> resolve(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return Optional.of(objectMapper.readValue(cached, CachedUrl.class));
            } catch (Exception e) {
                log.warn("Failed to parse cache for {}", shortCode);
            }
        }

        try {
            Optional<UrlMapping> mapping = urlMappingRepository.findByShortCodeAndDeletedFalse(shortCode);
            if (mapping.isEmpty()) {
                return Optional.empty();
            }

            UrlMapping url = mapping.get();
            CachedUrl cachedUrl = CachedUrl.builder()
                    .id(url.getId())
                    .longUrl(url.getLongUrl())
                    .active(url.isActive())
                    .expiresAt(url.getExpiresAt())
                    .build();

            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(cachedUrl), CACHE_TTL);
            } catch (Exception e) {
                log.warn("Failed to cache URL {}", shortCode);
            }

            return Optional.of(cachedUrl);
        } catch (Exception e) {
            log.error("Failed to resolve short code {} from database", shortCode, e);
            return Optional.empty();
        }
    }

    public void publishClickEvent(CachedUrl url, String shortCode, String ip, String userAgent, String referrer) {
        try {
            ClickEventMessage event = ClickEventMessage.builder()
                    .urlId(url.getId())
                    .shortCode(shortCode)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .referrer(referrer)
                    .timestamp(Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(AppConfig.EXCHANGE, AppConfig.CLICK_ROUTING_KEY, event);
            log.debug("Published click event for {}", shortCode);
        } catch (Exception e) {
            log.error("Failed to publish click event for {}: {}", shortCode, e.getMessage());
            incrementClickCountFallback(url.getId());
        }
    }

    /** Fallback when RabbitMQ is unavailable — keeps clickCount in sync for the links table */
    private void incrementClickCountFallback(UUID urlId) {
        if (urlId == null) {
            return;
        }
        try {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(urlId)),
                    new Update().inc("clickCount", 1),
                    "urls"
            );
        } catch (Exception e) {
            log.warn("Failed to increment click count for {}: {}", urlId, e.getMessage());
        }
    }
}
