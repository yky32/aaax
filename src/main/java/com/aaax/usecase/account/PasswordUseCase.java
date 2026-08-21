package com.aaax.usecase.account;

import java.util.Locale;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.spi.otp.OtpCodeStore;
import com.aaax.spi.otp.OtpSender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PasswordUseCase {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final OtpCodeStore otpStore;
    private final OtpSender otpSender;
    private final IdentityEventBus events;
    private final int otpTtlSeconds;
    private final int otpLength;

    public PasswordUseCase(
            AccountRepository accounts,
            PasswordEncoder passwordEncoder,
            OtpCodeStore otpStore,
            OtpSender otpSender,
            IdentityEventBus events,
            @Value("${aaax.otp.ttl-seconds:300}") int otpTtlSeconds,
            @Value("${aaax.otp.length:6}") int otpLength) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.otpStore = otpStore;
        this.otpSender = otpSender;
        this.events = events;
        this.otpTtlSeconds = otpTtlSeconds;
        this.otpLength = Math.max(4, Math.min(otpLength, 10));
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        Account account = require(username);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw AccountException.badRequest("current password incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw AccountException.badRequest("new password too short");
        }
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accounts.save(account);
        events.emit(IdentityEvent.Types.PASSWORD_CHANGED, username, java.util.Map.of());
    }

    @Transactional(readOnly = true)
    public void requestPasswordReset(String usernameOrEmail) {
        if (!StringUtils.hasText(usernameOrEmail)) {
            return;
        }
        String q = usernameOrEmail.trim();
        Account account = accounts.findByUsernameIgnoreCase(q)
                .or(() -> accounts.findByEmailIgnoreCase(q.toLowerCase(Locale.ROOT)))
                .orElse(null);
        if (account == null || !account.isEnabled()) {
            return;
        }
        String code = generateOtp();
        otpStore.put(resetKey(account.getUsername()), code, java.time.Instant.now().plusSeconds(otpTtlSeconds));
        String destination = account.getEmail() != null ? account.getEmail() : account.getUsername();
        otpSender.send(destination, code);
    }

    @Transactional
    public void resetPassword(String username, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw AccountException.badRequest("new password too short");
        }
        Account account = require(username);
        OtpCodeStore.Entry entry = otpStore.get(resetKey(account.getUsername()));
        if (entry == null || !entry.code().equals(code == null ? "" : code.trim())) {
            throw AccountException.badRequest("invalid or expired otp");
        }
        otpStore.remove(resetKey(account.getUsername()));
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accounts.save(account);
        events.emit(IdentityEvent.Types.PASSWORD_RESET, username, java.util.Map.of());
    }

    @Transactional(readOnly = true)
    public Account authenticatePassword(String username, String password) {
        Account account = accounts.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> AccountException.badRequest("invalid credentials"));
        if (!account.isEnabled() || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw AccountException.badRequest("invalid credentials");
        }
        return account;
    }

    private Account require(String username) {
        return accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int n = java.util.concurrent.ThreadLocalRandom.current().nextInt(bound / 10, bound);
        return String.valueOf(n);
    }

    static String resetKey(String username) {
        return "reset:" + username.trim().toLowerCase(Locale.ROOT);
    }
}
