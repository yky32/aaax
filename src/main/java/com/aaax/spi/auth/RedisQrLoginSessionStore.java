package com.aaax.spi.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import com.aaax.entity.po.QrLoginSession;

/**
 * Redis-backed QR sessions for multi-node. Keys: {@code aaax:qr:s:{id}}, {@code aaax:qr:c:{code}}.
 */
public class RedisQrLoginSessionStore implements QrLoginSessionStore {

    private static final String S_PREFIX = "aaax:qr:s:";
    private static final String C_PREFIX = "aaax:qr:c:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final int ttlSeconds;
    private final SecureRandom random = new SecureRandom();

    public RedisQrLoginSessionStore(StringRedisTemplate redis, ObjectMapper objectMapper, int ttlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public int ttlSeconds() {
        return ttlSeconds;
    }

    @Override
    public QrLoginSession create() {
        String id = newId(16);
        String code = newUserCode();
        QrLoginSession s = new QrLoginSession(id, code, Instant.now().plusSeconds(ttlSeconds));
        save(s);
        return s;
    }

    @Override
    public Optional<QrLoginSession> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String raw = redis.opsForValue().get(S_PREFIX + id.trim());
        if (raw == null) {
            return Optional.empty();
        }
        try {
            QrLoginSession s = fromMap(objectMapper.readValue(raw, Map.class));
            if (s.isExpired() && s.status() == QrLoginSession.Status.PENDING) {
                s.setStatus(QrLoginSession.Status.EXPIRED);
                save(s);
            }
            return Optional.of(s);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<QrLoginSession> getByUserCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String id = redis.opsForValue().get(C_PREFIX + code.trim().toUpperCase());
        return id == null ? Optional.empty() : get(id);
    }

    @Override
    public void save(QrLoginSession session) {
        try {
            long ttlMs = Math.max(1, Duration.between(Instant.now(), session.expiresAt()).toMillis());
            // keep a little past expiry for APPROVED consume race
            if (session.status() == QrLoginSession.Status.APPROVED) {
                ttlMs = Math.max(ttlMs, 30_000);
            }
            String json = objectMapper.writeValueAsString(toMap(session));
            redis.opsForValue().set(S_PREFIX + session.id(), json, ttlMs, TimeUnit.MILLISECONDS);
            redis.opsForValue().set(C_PREFIX + session.userCode(), session.id(), ttlMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("QR redis save failed", e);
        }
    }

    @Override
    public void remove(String id) {
        get(id).ifPresent(s -> {
            redis.delete(S_PREFIX + s.id());
            redis.delete(C_PREFIX + s.userCode());
        });
    }

    private static Map<String, Object> toMap(QrLoginSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.id());
        m.put("userCode", s.userCode());
        m.put("expiresAt", s.expiresAt().toString());
        m.put("status", s.status().name());
        m.put("approvedUsername", s.approvedUsername());
        return m;
    }

    @SuppressWarnings("unchecked")
    private static QrLoginSession fromMap(Map<?, ?> m) {
        String id = String.valueOf(m.get("id"));
        String code = String.valueOf(m.get("userCode"));
        Instant exp = Instant.parse(String.valueOf(m.get("expiresAt")));
        QrLoginSession s = new QrLoginSession(id, code, exp);
        Object st = m.get("status");
        if (st != null) {
            s.setStatus(QrLoginSession.Status.valueOf(String.valueOf(st)));
        }
        Object u = m.get("approvedUsername");
        if (u != null && !"null".equals(String.valueOf(u))) {
            s.setApprovedUsername(String.valueOf(u));
        }
        return s;
    }

    private String newId(int bytes) {
        byte[] b = new byte[bytes];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private String newUserCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
