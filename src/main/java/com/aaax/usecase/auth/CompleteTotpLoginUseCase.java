package com.aaax.usecase.auth;

import java.util.Map;

import com.aaax.entity.po.Account;
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
import org.springframework.web.server.ResponseStatusException;

@Component
public class CompleteTotpLoginUseCase {

    private final TotpMfaUseCase totp;
    private final AccountQueries queries;
    private final FinishAuthenticatedSession finish;
    private final IdentityEventBus events;
    private final TrustedDeviceUseCase devices;

    public CompleteTotpLoginUseCase(
            TotpMfaUseCase totp,
            AccountQueries queries,
            FinishAuthenticatedSession finish,
            IdentityEventBus events,
            TrustedDeviceUseCase devices) {
        this.totp = totp;
        this.queries = queries;
        this.finish = finish;
        this.events = events;
        this.devices = devices;
    }

    public Map<String, Object> execute(
            TotpCodeRequestDto body, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(PasswordLoginUseCase.MFA_PENDING_USER) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no pending mfa login");
        }
        String username = session.getAttribute(PasswordLoginUseCase.MFA_PENDING_USER).toString();
        boolean remember = Boolean.TRUE.equals(session.getAttribute(PasswordLoginUseCase.MFA_REMEMBER_DEVICE))
                || Boolean.TRUE.equals(body.rememberDevice());
        String label = body.deviceLabel();
        if (!totp.verify(username, body.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid totp code");
        }
        session.removeAttribute(PasswordLoginUseCase.MFA_PENDING_USER);
        session.removeAttribute(PasswordLoginUseCase.MFA_REMEMBER_DEVICE);
        Account account = queries.requireEntityByUsername(username);
        Map<String, Object> m =
                finish.execute(account, "password+totp", request, response, false);
        if (remember) {
            var d = devices.registerAndSetCookie(account, label, request, response);
            m.put("trustedDeviceId", d.getId());
            m.put("trustedDevice", true);
        }
        events.emit(
                IdentityEvent.Types.AUTH_LOGIN_MFA,
                username,
                "password+totp",
                Map.of("method", "password+totp", "sessionId", m.get("sessionId"), "rememberDevice", remember));
        return m;
    }
}
