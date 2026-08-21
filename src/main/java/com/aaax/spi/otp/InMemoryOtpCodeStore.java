package com.aaax.spi.otp;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aaax.otp.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryOtpCodeStore implements OtpCodeStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String code, Instant expiresAt) {
        store.put(normalize(key), new Entry(code, expiresAt));
    }

    @Override
    public Entry get(String key) {
        Entry entry = store.get(normalize(key));
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(normalize(key));
            return null;
        }
        return entry;
    }

    @Override
    public void remove(String key) {
        store.remove(normalize(key));
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase();
    }
}
