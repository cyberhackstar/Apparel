package com.ladiesapparel.common;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis-backed sliding-window-ish rate limiter (fixed-window via INCR + EXPIRE).
 * Shared across all application instances — unlike an in-memory map, this actually
 * enforces the limit correctly once the app is deployed behind a load balancer with
 * more than one instance.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "ratelimit:";

    /** Returns true if the request is allowed; false if the caller has exceeded the limit. */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        String redisKey = KEY_PREFIX + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return true; // fail-open if Redis is unreachable — don't take the whole site down
        }

        if (count == 1L) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        return count <= maxRequests;
    }
}
