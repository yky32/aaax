package com.aaax.usecase.auth;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.usecase.account.AccountQueries;
import com.aaax.entity.model.QrLoginSession;
import com.aaax.spi.auth.QrLoginSessionStore;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

/**
 * QR / device-pairing login: desktop creates session → phone (already signed in) approves → desktop consumes.
 */
@Component
public class QrLoginUseCase {

    private final QrLoginSessionStore qrLoginSessionStore;
    private final AccountQueries accountQueries;
    private final FinishAuthenticatedSession finishAuthenticatedSession;
    private final IdentityEventBus identityEventBus;
    private final String issuer;

    public QrLoginUseCase(
            QrLoginSessionStore qrLoginSessionStore,
            AccountQueries accountQueries,
            FinishAuthenticatedSession finishAuthenticatedSession,
            IdentityEventBus identityEventBus,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.qrLoginSessionStore = qrLoginSessionStore;
        this.accountQueries = accountQueries;
        this.finishAuthenticatedSession = finishAuthenticatedSession;
        this.identityEventBus = identityEventBus;
        this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    public Map<String, Object> create() {
        QrLoginSession s = qrLoginSessionStore.create();
        String approvePath = "/sign-in/qr-approve.html?sid=" + s.id();
        String approveUrl = issuer + approvePath;
        identityEventBus.emit(
                IdentityEvent.Types.AUTH_QR_CREATED,
                s.id(),
                Map.of("sessionId", s.id(), "expiresInSeconds", qrLoginSessionStore.ttlSeconds()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sessionId", s.id());
        m.put("userCode", s.userCode());
        m.put("status", s.status().name());
        m.put("expiresInSeconds", qrLoginSessionStore.ttlSeconds());
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
            throw new BizException(SystemResponse.SAU0403, "sign in on this device first");
        }
        QrLoginSession s = require(sessionId);
        if (s.status() == QrLoginSession.Status.EXPIRED || s.isExpired()) {
            s.setStatus(QrLoginSession.Status.EXPIRED);
            qrLoginSessionStore.save(s);
            throw new BizException(SystemResponse.PAM0400, "QR session expired");
        }
        if (s.status() == QrLoginSession.Status.CONSUMED) {
            throw new BizException(SystemResponse.PAM0400, "already consumed");
        }
        if (s.status() == QrLoginSession.Status.APPROVED) {
            return Map.of("sessionId", s.id(), "status", "APPROVED", "approvedUsername", s.approvedUsername());
        }
        // ensure account exists / active
        accountQueries.requireEntityByUsername(principal.getName());
        s.setApprovedUsername(principal.getName());
        s.setStatus(QrLoginSession.Status.APPROVED);
        qrLoginSessionStore.save(s);
        identityEventBus.emit(
                IdentityEvent.Types.AUTH_QR_APPROVED,
                principal.getName(),
                Map.of("sessionId", s.id(), "method", "qr"));
        return Map.of("sessionId", s.id(), "status", "APPROVED", "approvedUsername", principal.getName());
    }

    /** Approve by short user code (phone typed code). */
    public Map<String, Object> approveByCode(String userCode, Principal principal) {
        QrLoginSession s = qrLoginSessionStore.getByUserCode(userCode)
                .orElseThrow(() -> new BizException(SystemResponse.PAM0400, "unknown code"));
        return approve(s.id(), principal);
    }

    public Map<String, Object> consume(
            String sessionId, HttpServletRequest request, HttpServletResponse response) {
        QrLoginSession s = require(sessionId);
        if (s.status() == QrLoginSession.Status.EXPIRED || s.isExpired()) {
            s.setStatus(QrLoginSession.Status.EXPIRED);
            qrLoginSessionStore.save(s);
            throw new BizException(SystemResponse.PAM0400, "QR session expired");
        }
        if (s.status() != QrLoginSession.Status.APPROVED) {
            throw new BizException(SystemResponse.PAM0400, "not approved yet (status=" + s.status() + ")");
        }
        String username = s.approvedUsername();
        s.setStatus(QrLoginSession.Status.CONSUMED);
        qrLoginSessionStore.save(s);
        qrLoginSessionStore.remove(s.id());
        return finishAuthenticatedSession.execute(
                accountQueries.requireEntityByUsername(username),
                "qr",
                request,
                response,
                IdentityEvent.Types.AUTH_LOGIN,
                Map.of("method", "qr", "channel", "qr_code"));
    }

    private QrLoginSession require(String sessionId) {
        return qrLoginSessionStore.get(sessionId)
                .orElseThrow(() -> new BizException(SystemResponse.PAM0400, "unknown QR session"));
    }
}
