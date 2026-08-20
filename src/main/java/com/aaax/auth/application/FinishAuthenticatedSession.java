package com.aaax.auth.application;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.AccountResponse;
import com.aaax.account.AccountUserDetailsService;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.session.AuthSession;
import com.aaax.session.AuthSessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Shared login completion: Spring Security session + tracked AuthSession + optional event.
 */
@Component
public class FinishAuthenticatedSession {

    private final AccountUserDetailsService users;
    private final AuthSessionService authSessions;
    private final IdentityEventBus events;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public FinishAuthenticatedSession(
            AccountUserDetailsService users, AuthSessionService authSessions, IdentityEventBus events) {
        this.users = users;
        this.authSessions = authSessions;
        this.events = events;
    }

    public Map<String, Object> execute(
            Account account,
            String method,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean emitLoginEvent) {
        establishSpringSession(account.getUsername(), request, response);
        AuthSession tracked = authSessions.open(account.getId(), request);
        if (emitLoginEvent) {
            events.emit(
                    IdentityEvent.Types.AUTH_LOGIN,
                    account.getUsername(),
                    method,
                    Map.of("method", method, "sessionId", tracked.getId()));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mfaRequired", false);
        m.put("account", AccountResponse.from(account));
        m.put("sessionId", tracked.getId());
        return m;
    }

    private void establishSpringSession(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails user = users.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
