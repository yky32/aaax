package com.aaax.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountResponse register(RegisterAccountRequest request) {
        String username = request.username().trim();
        String email = normalizeEmail(request.email());
        String password = request.password();

        if (accountRepository.existsByUsernameIgnoreCase(username)) {
            throw AccountException.conflict("username already taken");
        }
        if (email != null && accountRepository.existsByEmailIgnoreCase(email)) {
            throw AccountException.conflict("email already registered");
        }

        Account account = new Account(username, email, passwordEncoder.encode(password));
        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public AccountResponse requireByUsername(String username) {
        return accountRepository.findByUsernameIgnoreCase(username)
                .map(AccountResponse::from)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    @Transactional(readOnly = true)
    public Account requireEntityByUsername(String username) {
        return accountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
