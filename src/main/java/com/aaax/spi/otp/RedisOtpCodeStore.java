package com.aaax.spi.otp;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis-backed OTP codes. Keys: {@code aaax:otp:{key}} with TTL.
 */
public class RedisOtpCodeStore implements OtpCodeStore {

    private static final String PREFIX = "aaax:otp:";

    private final StringRedisTemplate redis;

    public RedisOtpCodeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(String key, String code, Instant expiresAt) {
        long ttlMs = Math.max(1, Duration.between(Instant.now(), expiresAt).toMillis());
        redis.opsForValue().set(PREFIX + normalize(key), code, ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public Entry get(String key) {
        String k = PREFIX + normalize(key);
        String code = redis.opsForValue().get(k);
        if (code == null) {
            return null;
        }
        Long ttl = redis.getExpire(k, TimeUnit.SECONDS);
        Instant exp = ttl != null && ttl > 0
                ? Instant.now().plusSeconds(ttl)
                : Instant.now().plusSeconds(60);
        return new Entry(code, exp);
    }

    @Override
    public void remove(String key) {
        redis.delete(PREFIX + normalize(key));
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase();
    }
}
