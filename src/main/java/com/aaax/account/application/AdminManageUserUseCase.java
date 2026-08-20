package com.aaax.account.application;

import java.util.Locale;

import com.aaax.account.Account;
import com.aaax.account.AccountException;
import com.aaax.account.AccountRepository;
import com.aaax.account.AccountResponse;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminManageUserUseCase {

    private final AccountRepository accounts;
    private final IdentityEventBus events;

    public AdminManageUserUseCase(AccountRepository accounts, IdentityEventBus events) {
        this.accounts = accounts;
        this.events = events;
    }

    @Transactional
    public AccountResponse setEnabled(String id, boolean enabled, String actor) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        account.setEnabled(enabled);
        Account saved = accounts.save(account);
        events.emit(IdentityEvent.Types.USER_STATUS, actor, id + " enabled=" + enabled,
                java.util.Map.of("userId", id, "enabled", enabled));
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse setRoles(String id, String rolesCsv, String actor) {
        Account account = accounts.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        if (!StringUtils.hasText(rolesCsv)) {
            throw AccountException.badRequest("roles required");
        }
        account.setRoles(rolesCsv.trim().toUpperCase(Locale.ROOT));
        Account saved = accounts.save(account);
        events.emit(IdentityEvent.Types.USER_ROLES, actor, id + " -> " + saved.getRoles(),
                java.util.Map.of("userId", id, "roles", saved.getRoles()));
        return AccountResponse.from(saved);
    }
}
