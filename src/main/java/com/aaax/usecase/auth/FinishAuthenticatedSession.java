package com.aaax.usecase.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.entity.po.account.Account;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.service.AccountUserDetailsService;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.entity.po.session.AuthSession;
import com.aaax.usecase.session.AuthSessionUseCase;

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
 * Shared login completion for password / OTP / magic / social / SAML / passkey.
 * Spring Security session + tracked AuthSession + optional identity event.
 */
@Component
public class FinishAuthenticatedSession {

    private final AccountUserDetailsService accountUserDetailsService;
    private final AuthSessionUseCase authSessionUseCase;
    private final IdentityEventBus identityEventBus;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public FinishAuthenticatedSession(
            AccountUserDetailsService accountUserDetailsService, AuthSessionUseCase authSessionUseCase, IdentityEventBus identityEventBus) {
        this.accountUserDetailsService = accountUserDetailsService;
        this.authSessionUseCase = authSessionUseCase;
        this.identityEventBus = identityEventBus;
    }

    /** Default: emit {@link IdentityEvent.Types#AUTH_LOGIN}. */
    public Map<String, Object> execute(
            Account account,
            String method,
            HttpServletRequest request,
            HttpServletResponse response,
            boolean emitLoginEvent) {
        return execute(account, method, request, response,
                emitLoginEvent ? IdentityEvent.Types.AUTH_LOGIN : null,
                Map.of("method", method));
    }

    /**
     * @param eventType null = no event; otherwise emit with data (+ sessionId injected)
     */
    public Map<String, Object> execute(
            Account account,
            String method,
            HttpServletRequest request,
            HttpServletResponse response,
            String eventType,
            Map<String, Object> eventData) {
        establishSpringSession(account.getUsername(), request, response);
        AuthSession tracked = authSessionUseCase.open(account.getId(), request);
        if (eventType != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            if (eventData != null) {
                data.putAll(eventData);
            }
            data.putIfAbsent("method", method);
            data.put("sessionId", tracked.getId());
            identityEventBus.emit(eventType, account.getUsername(), method, data);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mfaRequired", false);
        m.put("account", GetAccountResponseDto.from(account));
        m.put("sessionId", tracked.getId());
        return m;
    }

    private void establishSpringSession(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails user = accountUserDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
