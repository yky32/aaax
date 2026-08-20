package com.aaax.otp;

import java.security.SecureRandom;
import java.time.Instant;

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
        var account = accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> AccountException.notFound("account not found"));
        String code = generateCode();
        Instant expires = Instant.now().plusSeconds(ttlSeconds);
        store.put(account.getUsername(), code, expires);
        String destination = account.getEmail() != null ? account.getEmail() : account.getUsername();
        sender.send(destination, code);
        return new OtpRequestResponse(account.getUsername(), destination, ttlSeconds, expires);
    }

    public OtpVerifyResponse verify(String username, String code) {
        if (code == null || code.isBlank()) {
            throw AccountException.badRequest("code required");
        }
        InMemoryOtpStore.Entry entry = store.get(username);
        if (entry == null || !entry.code().equals(code.trim())) {
            throw AccountException.badRequest("invalid or expired otp");
        }
        store.remove(username);
        return new OtpVerifyResponse(true, username.trim());
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, length);
        int n = random.nextInt(bound / 10, bound);
        return String.valueOf(n);
    }
}
