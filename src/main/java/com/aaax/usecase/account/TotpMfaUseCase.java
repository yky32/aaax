package com.aaax.usecase.account;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.dto.response.GetAccountResponseDto;
import com.aaax.entity.dto.response.TotpSetupResponseDto;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.service.TotpService;

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
    public TotpSetupResponseDto beginSetup(String username) {
        Account account = require(username);
        String secret = totpService.generateSecret();
        account.setTotpSecret(secret);
        account.setTotpEnabled(false);
        accounts.save(account);
        return new TotpSetupResponseDto(secret, totpService.otpAuthUrl("AAAX", account.getUsername(), secret));
    }

    @Transactional
    public GetAccountResponseDto confirm(String username, String code) {
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
        return GetAccountResponseDto.from(saved);
    }

    @Transactional
    public GetAccountResponseDto disable(String username, String password, String code) {
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
        return GetAccountResponseDto.from(saved);
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
