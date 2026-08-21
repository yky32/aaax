package com.aaax.auth.application;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.account.application.AccountQueries;
import com.aaax.auth.QrLoginSession;
import com.aaax.auth.QrLoginSessionStore;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * QR / device-pairing login: desktop creates session → phone (already signed in) approves → desktop consumes.
 */
@Component
public class QrLoginUseCase {

    private final QrLoginSessionStore store;
    private final AccountQueries queries;
    private final FinishAuthenticatedSession finish;
    private final IdentityEventBus events;
    private final String issuer;

    public QrLoginUseCase(
            QrLoginSessionStore store,
            AccountQueries queries,
            FinishAuthenticatedSession finish,
            IdentityEventBus events,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.store = store;
        this.queries = queries;
        this.finish = finish;
        this.events = events;
        this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    public Map<String, Object> create() {
        QrLoginSession s = store.create();
        String approvePath = "/sign-in/qr-approve.html?sid=" + s.id();
        String approveUrl = issuer + approvePath;
        events.emit(
                IdentityEvent.Types.AUTH_QR_CREATED,
                s.id(),
                Map.of("sessionId", s.id(), "expiresInSeconds", store.ttlSeconds()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", s.id());
        m.put("userCode", s.userCode());
        m.put("status", s.status().name());
        m.put("expiresInSeconds", store.ttlSeconds());
        m.put("expiresAt", s.expiresAt().toString());
        m.put("approveUrl", approveUrl);
        m.put("approvePath", approvePath);
        m.put("pollUrl", "/v1/auth/qr/sessions/" + s.id());
        m.put("consumeUrl", "/v1/auth/qr/sessions/" + s.id() + "/consume");
        return m;
    }

    public Map<String, Object> status(String sessionId) {
        QrLoginSession s = require(sessionId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", s.id());
        m.put("status", s.status().name());
        m.put("userCode", s.userCode());
        m.put("expiresAt", s.expiresAt().toString());
        if (s.status() == QrLoginSession.Status.APPROVED) {
            m.put("approvedUsername", s.approvedUsername());
        }
        return m;
    }

    public Map<String, Object> approve(String sessionId, Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "sign in on this device first");
        }
        QrLoginSession s = require(sessionId);
        if (s.status() == QrLoginSession.Status.EXPIRED || s.isExpired()) {
            s.setStatus(QrLoginSession.Status.EXPIRED);
            store.save(s);
            throw new ResponseStatusException(HttpStatus.GONE, "QR session expired");
        }
        if (s.status() == QrLoginSession.Status.CONSUMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "already consumed");
        }
        if (s.status() == QrLoginSession.Status.APPROVED) {
            return Map.of("sessionId", s.id(), "status", "APPROVED", "approvedUsername", s.approvedUsername());
        }
        // ensure account exists / active
        queries.requireEntityByUsername(principal.getName());
        s.setApprovedUsername(principal.getName());
        s.setStatus(QrLoginSession.Status.APPROVED);
        store.save(s);
        events.emit(
                IdentityEvent.Types.AUTH_QR_APPROVED,
                principal.getName(),
                Map.of("sessionId", s.id(), "method", "qr"));
        return Map.of("sessionId", s.id(), "status", "APPROVED", "approvedUsername", principal.getName());
    }

    /** Approve by short user code (phone typed code). */
    public Map<String, Object> approveByCode(String userCode, Principal principal) {
        QrLoginSession s = store.getByUserCode(userCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown code"));
        return approve(s.id(), principal);
    }

    public Map<String, Object> consume(
            String sessionId, HttpServletRequest request, HttpServletResponse response) {
        QrLoginSession s = require(sessionId);
        if (s.status() == QrLoginSession.Status.EXPIRED || s.isExpired()) {
            s.setStatus(QrLoginSession.Status.EXPIRED);
            store.save(s);
            throw new ResponseStatusException(HttpStatus.GONE, "QR session expired");
        }
        if (s.status() != QrLoginSession.Status.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "not approved yet (status=" + s.status() + ")");
        }
        String username = s.approvedUsername();
        s.setStatus(QrLoginSession.Status.CONSUMED);
        store.save(s);
        store.remove(s.id());
        return finish.execute(
                queries.requireEntityByUsername(username),
                "qr",
                request,
                response,
                IdentityEvent.Types.AUTH_LOGIN,
                Map.of("method", "qr", "channel", "qr_code"));
    }

    private QrLoginSession require(String sessionId) {
        return store.get(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown QR session"));
    }
}
