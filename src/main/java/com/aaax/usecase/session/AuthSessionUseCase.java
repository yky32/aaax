package com.aaax.usecase.session;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.aaax.entity.po.session.AuthSession;
import com.aaax.repository.AuthSessionRepository;

@Component
public class AuthSessionUseCase {

    private final AuthSessionRepository authSessionRepository;
    private final SecureRandom random = new SecureRandom();

    public AuthSessionUseCase(AuthSessionRepository authSessionRepository) {
        this.authSessionRepository = authSessionRepository;
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
        return authSessionRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<AuthSession> listActive(String accountId) {
        return authSessionRepository.findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(accountId);
    }

    @Transactional
    public void revoke(String accountId, String sessionId) {
        authSessionRepository.findById(sessionId).ifPresent(s -> {
            if (accountId.equals(s.getAccountId()) && s.isSessionActive()) {
                s.setRevokedAt(Instant.now());
                authSessionRepository.save(s);
            }
        });
    }

    @Transactional
    public void revokeAll(String accountId) {
        for (AuthSession s : listActive(accountId)) {
            s.setRevokedAt(Instant.now());
            authSessionRepository.save(s);
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
