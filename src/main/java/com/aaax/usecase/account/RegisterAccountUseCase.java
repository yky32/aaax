package com.aaax.usecase.account;

import java.util.Locale;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.AccountResponse;
import com.aaax.entity.dto.request.RegisterAccountRequest;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class RegisterAccountUseCase {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final IdentityEventBus events;

    public RegisterAccountUseCase(
            AccountRepository accounts, PasswordEncoder passwordEncoder, IdentityEventBus events) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Transactional
    public AccountResponse execute(RegisterAccountRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        if (accounts.existsByUsernameIgnoreCase(username)) {
            throw AccountException.conflict("username already taken");
        }
        if (email != null && accounts.existsByEmailIgnoreCase(email)) {
            throw AccountException.conflict("email already registered");
        }
        Account saved = accounts.save(new Account(username, email, passwordEncoder.encode(request.password())));
        events.emit(IdentityEvent.Types.ACCOUNT_REGISTERED, username, "self-register",
                java.util.Map.of("email", email == null ? "" : email));
        return AccountResponse.from(saved);
    }

    static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
