package com.aaax.session;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSessionService {

    private final AuthSessionRepository repository;
    private final SecureRandom random = new SecureRandom();

    public AuthSessionService(AuthSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AuthSession open(String accountId, HttpServletRequest request) {
        AuthSession s = new AuthSession();
        s.setId(UUID.randomUUID().toString());
        s.setAccountId(accountId);
        s.setSessionToken(newToken());
        if (request != null) {
            s.setUserAgent(trim(request.getHeader("User-Agent"), 512));
            s.setIp(trim(request.getRemoteAddr(), 64));
        }
        return repository.save(s);
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listActive(String accountId) {
        return repository.findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(accountId);
    }

    @Transactional
    public void revoke(String accountId, String sessionId) {
        repository.findById(sessionId).ifPresent(s -> {
            if (accountId.equals(s.getAccountId()) && s.isActive()) {
                s.setRevokedAt(Instant.now());
                repository.save(s);
            }
        });
    }

    @Transactional
    public void revokeAll(String accountId) {
        for (AuthSession s : listActive(accountId)) {
            s.setRevokedAt(Instant.now());
            repository.save(s);
        }
    }

    private String newToken() {
        byte[] buf = new byte[24];
        random.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static String trim(String v, int max) {
        if (v == null) {
            return null;
        }
        return v.length() <= max ? v : v.substring(0, max);
    }
}
