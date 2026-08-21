package com.aaax.spi.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

/** Redis magic-link tokens. Keys: {@code aaax:magic:{token}}. */
public class RedisMagicLinkTokenStore implements MagicLinkTokenStore {

    private static final String PREFIX = "aaax:magic:";

    private final StringRedisTemplate redis;

    public RedisMagicLinkTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(String token, String username, Instant expiresAt) {
        long ttlMs = Math.max(1, Duration.between(Instant.now(), expiresAt).toMillis());
        redis.opsForValue().set(PREFIX + token, username, ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<String> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String key = PREFIX + token.trim();
        String username = redis.opsForValue().getAndDelete(key);
        return Optional.ofNullable(username);
    }
}
