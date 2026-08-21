package com.aaax.endpoint.session;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aaax.entity.po.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.po.AuthSession;
import com.aaax.service.AuthSessionService;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/sessions")
@PreAuthorize("isAuthenticated()")
public class SessionEndpoint {

    private final AuthSessionService sessions;
    private final AccountRepository accounts;

    public SessionEndpoint(AuthSessionService sessions, AccountRepository accounts) {
        this.sessions = sessions;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Map<String, Object>> list(Principal principal) {
        Account a = require(principal);
        return sessions.listActive(a.getId()).stream().map(this::toMap).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String id, Principal principal) {
        Account a = require(principal);
        sessions.revoke(a.getId(), id);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAll(Principal principal) {
        Account a = require(principal);
        sessions.revokeAll(a.getId());
    }

    private Account require(Principal principal) {
        return accounts.findByUsernameIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Map<String, Object> toMap(AuthSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userAgent", s.getUserAgent());
        m.put("ip", s.getIp());
        m.put("createdAt", s.getCreatedAt());
        m.put("lastSeenAt", s.getLastSeenAt());
        return m;
    }
}
