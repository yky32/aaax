package com.aaax.usecase.auth;

import java.util.Map;

import com.aaax.entity.po.account.Account;
import com.aaax.entity.dto.request.TotpCodeRequestDto;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.usecase.account.TotpMfaUseCase;
import com.aaax.usecase.device.TrustedDeviceUseCase;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;

@Component
public class CompleteTotpLoginUseCase {

    private final TotpMfaUseCase totpMfaUseCase;
    private final AccountQueries accountQueries;
    private final FinishAuthenticatedSession finishAuthenticatedSession;
    private final IdentityEventBus events;
    private final TrustedDeviceUseCase trustedDeviceUseCase;

    public CompleteTotpLoginUseCase(
            TotpMfaUseCase totpMfaUseCase,
            AccountQueries accountQueries,
            FinishAuthenticatedSession finishAuthenticatedSession,
            IdentityEventBus events,
            TrustedDeviceUseCase trustedDeviceUseCase) {
        this.totpMfaUseCase = totpMfaUseCase;
        this.accountQueries = accountQueries;
        this.finishAuthenticatedSession = finishAuthenticatedSession;
        this.events = events;
        this.trustedDeviceUseCase = trustedDeviceUseCase;
    }

    public Map<String, Object> execute(
            TotpCodeRequestDto body, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(PasswordLoginUseCase.MFA_PENDING_USER) == null) {
            throw new BizException(SystemResponse.PAM0400, "no pending mfa login");
        }
        String username = session.getAttribute(PasswordLoginUseCase.MFA_PENDING_USER).toString();
        boolean remember = Boolean.TRUE.equals(session.getAttribute(PasswordLoginUseCase.MFA_REMEMBER_DEVICE))
                || Boolean.TRUE.equals(body.rememberDevice());
        String label = body.deviceLabel();
        if (!totpMfaUseCase.verify(username, body.code())) {
            throw new BizException(SystemResponse.PAM0400, "invalid totp code");
        }
        session.removeAttribute(PasswordLoginUseCase.MFA_PENDING_USER);
        session.removeAttribute(PasswordLoginUseCase.MFA_REMEMBER_DEVICE);
        Account account = accountQueries.requireEntityByUsername(username);
        Map<String, Object> m =
                finishAuthenticatedSession.execute(account, "password+totpMfaUseCase", request, response, false);
        if (remember) {
            var d = trustedDeviceUseCase.registerAndSetCookie(account, label, request, response);
            m.put("trustedDeviceId", d.getId());
            m.put("trustedDevice", true);
        }
        events.emit(
                IdentityEvent.Types.AUTH_LOGIN_MFA,
                username,
                "password+totpMfaUseCase",
                Map.of("method", "password+totpMfaUseCase", "sessionId", m.get("sessionId"), "rememberDevice", remember));
        return m;
    }
}
