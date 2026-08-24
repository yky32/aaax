package com.aaax.endpoint.session;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aaax.entity.po.account.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.po.session.AuthSession;
import com.aaax.usecase.session.AuthSessionUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.exception.response.AccountErrorResponse;

@RestController
@RequestMapping("/v1/sessions")
@PreAuthorize("isAuthenticated()")
public class SessionEndpoint {

    private final AuthSessionUseCase authSessionUseCase;
    private final AccountRepository accountRepository;

    public SessionEndpoint(AuthSessionUseCase authSessionUseCase, AccountRepository accountRepository) {
        this.authSessionUseCase = authSessionUseCase;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(Principal principal) {
        Account a = require(principal);
        return authSessionUseCase.listActive(a.getId()).stream().map(this::toMap).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String id, Principal principal) {
        Account a = require(principal);
        authSessionUseCase.revoke(a.getId(), id);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAll(Principal principal) {
        Account a = require(principal);
        authSessionUseCase.revokeAll(a.getId());
    }

    private Account require(Principal principal) {
        return accountRepository.findByUsernameIgnoreCase(principal.getName())
                .orElseThrow(() -> new BizException(SystemResponse.SAU0403, "unauthorized"));
    }

    private Map<String, Object> toMap(AuthSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userAgent", s.getUserAgent());
        m.put("ip", s.getIp());
        m.put("createDt", s.getCreateDt());
        m.put("lastSeenAt", s.getLastSeenAt());
        return m;
    }
}
