package com.aaax.otp;

import java.security.SecureRandom;
import java.time.Instant;

import com.aaax.account.Account;
import com.aaax.account.AccountException;
import com.aaax.account.AccountRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private final AccountRepository accountRepository;
    private final InMemoryOtpStore store;
    private final OtpSender sender;
    private final int ttlSeconds;
    private final int length;
    private final SecureRandom random = new SecureRandom();

    public OtpService(
            AccountRepository accountRepository,
            InMemoryOtpStore store,
            OtpSender sender,
            @Value("${aaax.otp.ttl-seconds:300}") int ttlSeconds,
            @Value("${aaax.otp.length:6}") int length) {
        this.accountRepository = accountRepository;
        this.store = store;
        this.sender = sender;
        this.ttlSeconds = ttlSeconds;
        this.length = Math.max(4, Math.min(length, 10));
    }

    @Transactional(readOnly = true)
    public OtpRequestResponse request(String username) {
        Account account = requireAccount(username);
        String code = generateCode();
        Instant expires = Instant.now().plusSeconds(ttlSeconds);
        store.put(account.getUsername(), code, expires);
        String destination = account.getEmail() != null ? account.getEmail() : account.getUsername();
        sender.send(destination, code);
        return new OtpRequestResponse(account.getUsername(), maskDestination(destination), ttlSeconds, expires);
    }

    /**
     * Verify OTP without consuming side-effects beyond removal. Returns the account username (canonical).
     */
    @Transactional(readOnly = true)
    public Account verifyForLogin(String username, String code) {
        if (code == null || code.isBlank()) {
            throw AccountException.badRequest("code required");
        }
        Account account = requireAccount(username);
        InMemoryOtpStore.Entry entry = store.get(account.getUsername());
        if (entry == null || !entry.code().equals(code.trim())) {
            throw AccountException.badRequest("invalid or expired otp");
        }
        store.remove(account.getUsername());
        if (!account.isEnabled()) {
            throw AccountException.badRequest("account disabled");
        }
        return account;
    }

    public OtpVerifyResponse verify(String username, String code) {
        Account account = verifyForLogin(username, code);
        return new OtpVerifyResponse(true, account.getUsername());
    }

    private Account requireAccount(String username) {
        return accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, length);
        int n = random.nextInt(bound / 10, bound);
        return String.valueOf(n);
    }

    private static String maskDestination(String destination) {
        if (destination == null || !destination.contains("@")) {
            return destination;
        }
        int at = destination.indexOf('@');
        String local = destination.substring(0, at);
        String domain = destination.substring(at);
        if (local.length() <= 2) {
            return "*" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
