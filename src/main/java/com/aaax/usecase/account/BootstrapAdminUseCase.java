package com.aaax.usecase.account;

import com.aaax.entity.po.account.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.request.BootstrapAdminRequestDto;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminUseCase {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventBus identityEventBus;
    private final String configuredToken;

    public BootstrapAdminUseCase(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            IdentityEventBus identityEventBus,
            @Value("${aaax.bootstrap.token:}") String configuredToken) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.identityEventBus = identityEventBus;
        this.configuredToken = configuredToken;
    }

    @Transactional
    public GetAccountResponseDto execute(BootstrapAdminRequestDto body) {
        if (accountRepository.countByRolesContainingIgnoreCase("ADMIN") > 0) {
            throw AccountException.conflict("admin already exists");
        }
        if (StringUtils.hasText(configuredToken) && !configuredToken.equals(body.bootstrapToken())) {
            throw AccountException.badRequest("invalid bootstrap token");
        }
        if (!StringUtils.hasText(body.username()) || !StringUtils.hasText(body.password()) || body.password().length() < 8) {
            throw AccountException.badRequest("username and password (min 8) required");
        }
        if (accountRepository.existsByUsernameIgnoreCase(body.username().trim())) {
            throw AccountException.conflict("username already taken");
        }
        Account account = new Account(
                body.username().trim(),
                RegisterAccountUseCase.normalizeEmail(body.email()),
                passwordEncoder.encode(body.password()),
                "USER,ADMIN");
        Account saved = accountRepository.save(account);
        identityEventBus.emit(IdentityEvent.Types.BOOTSTRAP_ADMIN, saved.getUsername(), "first admin",
                java.util.Map.of("roles", "USER,ADMIN"));
        return GetAccountResponseDto.from(saved);
    }

    public boolean tokenRequired() {
        return StringUtils.hasText(configuredToken);
    }
}
