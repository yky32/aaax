package com.aaax.otp;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryOtpStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public void put(String key, String code, Instant expiresAt) {
        store.put(normalize(key), new Entry(code, expiresAt));
    }

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

    public void remove(String key) {
        store.remove(normalize(key));
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase();
    }

    public record Entry(String code, Instant expiresAt) {
    }
}
