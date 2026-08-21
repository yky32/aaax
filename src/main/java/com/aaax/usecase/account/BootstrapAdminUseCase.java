package com.aaax.usecase.account;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.AccountResponse;
import com.aaax.entity.dto.AccountDtos.BootstrapAdminRequest;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.aaax.entity.dto.AccountDtos;

@Component
public class BootstrapAdminUseCase {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventBus events;
    private final String configuredToken;

    public BootstrapAdminUseCase(
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            IdentityEventBus events,
            @Value("${aaax.bootstrap.token:}") String configuredToken) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.configuredToken = configuredToken;
    }

    @Transactional
    public AccountResponse execute(BootstrapAdminRequest body) {
        if (accounts.countByRolesContainingIgnoreCase("ADMIN") > 0) {
            throw AccountException.conflict("admin already exists");
        }
        if (StringUtils.hasText(configuredToken) && !configuredToken.equals(body.bootstrapToken())) {
            throw AccountException.badRequest("invalid bootstrap token");
        }
        if (!StringUtils.hasText(body.username()) || !StringUtils.hasText(body.password()) || body.password().length() < 8) {
            throw AccountException.badRequest("username and password (min 8) required");
        }
        if (accounts.existsByUsernameIgnoreCase(body.username().trim())) {
            throw AccountException.conflict("username already taken");
        }
        Account account = new Account(
                body.username().trim(),
                RegisterAccountUseCase.normalizeEmail(body.email()),
                passwordEncoder.encode(body.password()),
                "USER,ADMIN");
        Account saved = accounts.save(account);
        events.emit(IdentityEvent.Types.BOOTSTRAP_ADMIN, saved.getUsername(), "first admin",
                java.util.Map.of("roles", "USER,ADMIN"));
        return AccountResponse.from(saved);
    }

    public boolean tokenRequired() {
        return StringUtils.hasText(configuredToken);
    }
}
