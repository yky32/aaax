package com.aaax.core.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisUtilGetOrLoadTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;
    @Mock
    ValueOperations<String, Object> valueOperations;

    RedisUtil redisUtil;

    @BeforeEach
    void setUp() {
        redisUtil = new RedisUtil(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getOrLoad_returnsCachedWhenPresent() {
        when(valueOperations.get("k")).thenReturn("cached");
        AtomicInteger loads = new AtomicInteger();

        String result = redisUtil.getOrLoad("k", String.class, () -> {
            loads.incrementAndGet();
            return "loaded";
        });

        assertEquals("cached", result);
        assertEquals(0, loads.get());
    }

    @Test
    void getOrLoad_loadsWhenCacheNull() {
        when(valueOperations.get("k")).thenReturn(null);
        AtomicInteger loads = new AtomicInteger();

        String result = redisUtil.getOrLoad("k", String.class, () -> {
            loads.incrementAndGet();
            return "loaded";
        });

        assertEquals("loaded", result);
        assertEquals(1, loads.get());
    }
}
