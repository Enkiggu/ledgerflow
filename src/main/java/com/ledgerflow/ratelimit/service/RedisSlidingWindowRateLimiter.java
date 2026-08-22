package com.ledgerflow.ratelimit.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSlidingWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisSlidingWindowRateLimiter.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Counter rateLimitRejectionsCounter;

    public record RateLimitResult(
            boolean allowed,
            long limit,
            long remaining,
            long resetSeconds
    ) {}

    public RedisSlidingWindowRateLimiter(RedisTemplate<String, Object> redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.rateLimitRejectionsCounter = meterRegistry.counter("rate.limit.rejections");
    }

    public RateLimitResult isAllowed(String keyPrefix, String clientId, int limitPerMinute) {
        String redisKey = "ratelimit:" + keyPrefix + ":" + clientId;
        long now = System.currentTimeMillis();
        long windowStart = now - 60000L;

        try {
            // 1. Remove expired timestamps outside the sliding 60s window
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            // 2. Count active hits in current window
            Long currentCount = redisTemplate.opsForZSet().zCard(redisKey);
            long count = currentCount != null ? currentCount : 0;

            if (count < limitPerMinute) {
                // 3. Add current hit
                String member = now + ":" + UUID.randomUUID().toString().substring(0, 8);
                redisTemplate.opsForZSet().add(redisKey, member, now);
                redisTemplate.expire(redisKey, 65, TimeUnit.SECONDS);

                long remaining = Math.max(0, limitPerMinute - count - 1);
                return new RateLimitResult(true, limitPerMinute, remaining, 60);
            } else {
                rateLimitRejectionsCounter.increment();
                log.warn("Rate limit exceeded for key: {} (count: {}, limit: {})", redisKey, count, limitPerMinute);
                return new RateLimitResult(false, limitPerMinute, 0, 60);
            }
        } catch (Exception e) {
            // Resilient Fallback: If Redis is down, fail open to prevent blocking business traffic
            log.error("Redis rate limiter unavailable. Failing open gracefully for client: {}", clientId, e);
            return new RateLimitResult(true, limitPerMinute, limitPerMinute, 60);
        }
    }
}
