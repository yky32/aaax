package com.aaax.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory QR login sessions (single-node). Multi-node later: shared store like OTP Redis.
 */
@Component
public class QrLoginSessionStore {

    private final Map<String, QrLoginSession> byId = new ConcurrentHashMap<>();
    private final Map<String, String> codeToId = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final int ttlSeconds;

    public QrLoginSessionStore(@Value("${aaax.qr.ttl-seconds:120}") int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int ttlSeconds() {
        return ttlSeconds;
    }

    public QrLoginSession create() {
        String id = newId(16);
        String code = newUserCode();
        QrLoginSession s = new QrLoginSession(id, code, Instant.now().plusSeconds(ttlSeconds));
        byId.put(id, s);
        codeToId.put(code, id);
        return s;
    }

    public Optional<QrLoginSession> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        QrLoginSession s = byId.get(id.trim());
        if (s == null) {
            return Optional.empty();
        }
        if (s.isExpired() && s.status() == QrLoginSession.Status.PENDING) {
            s.setStatus(QrLoginSession.Status.EXPIRED);
        }
        return Optional.of(s);
    }

    public Optional<QrLoginSession> getByUserCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String id = codeToId.get(code.trim().toUpperCase());
        return id == null ? Optional.empty() : get(id);
    }

    public void remove(String id) {
        get(id).ifPresent(s -> {
            byId.remove(s.id());
            codeToId.remove(s.userCode());
        });
    }

    private String newId(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    /** Short human code for manual entry (e.g. phone without camera). */
    private String newUserCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
