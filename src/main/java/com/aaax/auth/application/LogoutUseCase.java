package com.aaax.auth.application;

import java.security.Principal;
import java.util.Map;

import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LogoutUseCase {

    private final IdentityEventBus events;

    public LogoutUseCase(IdentityEventBus events) {
        this.events = events;
    }

    public void execute(HttpServletRequest request, Principal principal) {
        if (principal != null) {
            events.emit(IdentityEvent.Types.AUTH_LOGOUT, principal.getName(), Map.of());
        }
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
