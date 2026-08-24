package com.aaax.usecase.account;

import java.util.Locale;

import com.aaax.entity.po.account.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminManageUserUseCase {

    private final AccountRepository accountRepository;
    private final IdentityEventBus identityEventBus;

    public AdminManageUserUseCase(AccountRepository accountRepository, IdentityEventBus identityEventBus) {
        this.accountRepository = accountRepository;
        this.identityEventBus = identityEventBus;
    }

    @Transactional
    public GetAccountResponseDto setEnabled(String id, boolean enabled, String actor) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        account.setEnabled(enabled);
        Account saved = accountRepository.save(account);
        identityEventBus.emit(IdentityEvent.Types.USER_STATUS, actor, id + " enabled=" + enabled,
                java.util.Map.of("userId", id, "enabled", enabled));
        return GetAccountResponseDto.from(saved);
    }

    @Transactional
    public GetAccountResponseDto setRoles(String id, String rolesCsv, String actor) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> AccountException.notFound("account not found"));
        if (!StringUtils.hasText(rolesCsv)) {
            throw AccountException.badRequest("roles required");
        }
        account.setRoles(rolesCsv.trim().toUpperCase(Locale.ROOT));
        Account saved = accountRepository.save(account);
        identityEventBus.emit(IdentityEvent.Types.USER_ROLES, actor, id + " -> " + saved.getRoles(),
                java.util.Map.of("userId", id, "roles", saved.getRoles()));
        return GetAccountResponseDto.from(saved);
    }
}
