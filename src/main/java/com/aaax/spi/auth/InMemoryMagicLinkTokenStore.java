package com.aaax.spi.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aaax.otp.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryMagicLinkTokenStore implements MagicLinkTokenStore {

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    @Override
    public void put(String token, String username, Instant expiresAt) {
        tokens.put(token, new Entry(username, expiresAt));
    }

    @Override
    public Optional<String> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Entry e = tokens.remove(token.trim());
        if (e == null || Instant.now().isAfter(e.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(e.username());
    }

    private record Entry(String username, Instant expiresAt) {
    }
}
