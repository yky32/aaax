package com.aaax.account.application;

import com.aaax.account.Account;
import com.aaax.account.AccountException;
import com.aaax.account.AccountRepository;
import com.aaax.account.AccountResponse;
import com.aaax.account.application.AccountDtos.TotpSetupResponse;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.mfa.TotpService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class TotpMfaUseCase {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final IdentityEventBus events;

    public TotpMfaUseCase(
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            TotpService totpService,
            IdentityEventBus events) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.events = events;
    }

    @Transactional
    public TotpSetupResponse beginSetup(String username) {
        Account account = require(username);
        String secret = totpService.generateSecret();
        account.setTotpSecret(secret);
        account.setTotpEnabled(false);
        accounts.save(account);
        return new TotpSetupResponse(secret, totpService.otpAuthUrl("AAAX", account.getUsername(), secret));
    }

    @Transactional
    public AccountResponse confirm(String username, String code) {
        Account account = require(username);
        if (!StringUtils.hasText(account.getTotpSecret())) {
            throw AccountException.badRequest("totp setup not started");
        }
        if (!totpService.verify(account.getTotpSecret(), code)) {
            throw AccountException.badRequest("invalid totp code");
        }
        account.setTotpEnabled(true);
        Account saved = accounts.save(account);
        events.emit(IdentityEvent.Types.MFA_TOTP_ENABLED, username, java.util.Map.of());
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse disable(String username, String password, String code) {
        Account account = require(username);
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw AccountException.badRequest("password incorrect");
        }
        if (account.isTotpEnabled() && !totpService.verify(account.getTotpSecret(), code)) {
            throw AccountException.badRequest("invalid totp code");
        }
        account.setTotpEnabled(false);
        account.setTotpSecret(null);
        Account saved = accounts.save(account);
        events.emit(IdentityEvent.Types.MFA_TOTP_DISABLED, username, java.util.Map.of());
        return AccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public boolean verify(String username, String code) {
        Account account = require(username);
        if (!account.isTotpEnabled()) {
            return true;
        }
        return totpService.verify(account.getTotpSecret(), code);
    }

    private Account require(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }
}
