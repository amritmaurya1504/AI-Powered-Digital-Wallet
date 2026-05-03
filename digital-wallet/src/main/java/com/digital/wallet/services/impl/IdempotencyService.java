package com.digital.wallet.services.impl;


import com.digital.wallet.dtos.IdempotencyRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    // Logger for debugging and monitoring idempotency behavior
    private final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    // RedisTemplate for interacting with Redis (GET, SET, DELETE)
    private final RedisTemplate<String, String> redisTemplate;

    // ObjectMapper for converting Java objects <-> JSON
    private final ObjectMapper objectMapper;

    // TTL for COMPLETED state (default = 86400 sec = 24 hours)
    private final long cacheTtlSeconds;

    // Prefix to avoid key collision in Redis
    // Example final key: idempotency:txn-123-user1
    private static final String CACHE_PREFIX = "idempotency:";

    public IdempotencyService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper
    ,@Value("${idempotency.cache-ttl-seconds:86400}") long cacheTtlSeconds){
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    // ============================================================
    // STEP 1: CHECK - Redis me record already exist karta hai ya nahi
    // ============================================================
    public IdempotencyRecord getRecord(String idempotencyKey) {

        // Build final Redis key
        String cacheKey = buildCacheKey(idempotencyKey);

        // Fetch data from Redis
        String cached = redisTemplate.opsForValue().get(cacheKey);

        // Agar kuch nahi mila → new request
        if (cached == null) return null;

        try {
            // JSON → Java object convert karo
            return objectMapper.readValue(cached, IdempotencyRecord.class);

        } catch (Exception e) {
            // Agar parsing fail ho gaya → corrupted data
            log.error("[IDEMPOTENCY] Failed to parse cached record for key={}", idempotencyKey, e);

            // Safe fallback: treat as no record
            return null;
        }
    }

    // ============================================================
    // STEP 2: MARK AS PROCESSING (LOCK ACQUIRE)
    // ============================================================
    public void markAsProcessing(String idempotencyKey) {

        // Create a record with PROCESSING status
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(idempotencyKey)
                .status("PROCESSING")
                .responseBody(null) // abhi response nahi bana
                .build();

        // Store in Redis WITHOUT TTL
        // ⚠️ Ye ek placeholder + lock ka kaam karega
        redisTemplate.opsForValue().set(
                buildCacheKey(idempotencyKey),
                toJson(record),
                Duration.ofMinutes(2)
        );

        log.info("[IDEMPOTENCY] Marked as PROCESSING, key={}", idempotencyKey);
    }

    // ============================================================
    // STEP 3: MARK AS COMPLETED (FINAL SUCCESS + CACHE)
    // ============================================================
    public void markAsCompleted(String idempotencyKey, Object responseBody) {

        // Create final record with full response
        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(idempotencyKey)
                .status("COMPLETED")
                .responseBody(responseBody) // 🔥 FULL RESPONSE STORE
                .build();

        // Store in Redis WITH TTL (24 hours)
        // ⚡ Retry requests yahi se serve honge (no DB hit)
        redisTemplate.opsForValue().set(
                buildCacheKey(idempotencyKey),
                toJson(record),
                Duration.ofSeconds(cacheTtlSeconds)
        );

        log.info("[IDEMPOTENCY] Marked as COMPLETED, key={}, ttl={}s", idempotencyKey, cacheTtlSeconds);
    }

    // ============================================================
    // STEP 4: DELETE RECORD (FAILURE CASE)
    // ============================================================
    public void deleteRecord(String idempotencyKey) {

        // Agar transaction fail ho gaya → record hata do
        // Taaki retry fresh request treat ho
        redisTemplate.delete(buildCacheKey(idempotencyKey));

        log.warn("[IDEMPOTENCY] Record deleted (failed), key={}", idempotencyKey);
    }

    // ============================================================
    // HELPER: Build Redis Key
    // ============================================================
    private String buildCacheKey(String idempotencyKey) {

        // Prefix + actual key combine
        return CACHE_PREFIX + idempotencyKey;
    }

    // ============================================================
    // HELPER: Convert Object → JSON
    // ============================================================
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize idempotency record", e);
        }
    }
}