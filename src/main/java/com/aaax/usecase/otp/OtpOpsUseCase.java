package com.aaax.usecase.otp;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.aaax.entity.dto.response.RequestOtpResponseDto;
import com.aaax.entity.dto.response.VerifyOtpResponseDto;
import com.aaax.spi.otp.OtpCodeStore;
import com.aaax.spi.otp.OtpSender;

@Component
public class OtpOpsUseCase {

    private final AccountRepository accountRepository;
    private final OtpCodeStore store;
    private final OtpSender sender;
    private final IdentityEventBus events;
    private final String channel;
    private final int ttlSeconds;
    private final int length;
    private final SecureRandom random = new SecureRandom();

    public OtpOpsUseCase(
            AccountRepository accountRepository,
            OtpCodeStore store,
            OtpSender sender,
            IdentityEventBus events,
            @Value("${aaax.otp.channel:console}") String channel,
            @Value("${aaax.otp.ttl-seconds:300}") int ttlSeconds,
            @Value("${aaax.otp.length:6}") int length) {
        this.accountRepository = accountRepository;
        this.store = store;
        this.sender = sender;
        this.events = events;
        this.channel = channel;
        this.ttlSeconds = ttlSeconds;
        this.length = Math.max(4, Math.min(length, 10));
    }

    @Transactional(readOnly = true)
    public RequestOtpResponseDto request(String username) {
        Account account = requireAccount(username);
        String code = generateCode();
        Instant expires = Instant.now().plusSeconds(ttlSeconds);
        store.put(account.getUsername(), code, expires);
        String destination = resolveDestination(account);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("destination", destination);
        data.put("channel", channel);
        data.put("code", code);
        data.put("purpose", "login_otp");
        data.put("expiresAt", expires.toString());
        data.put("username", account.getUsername());
        events.emit(IdentityEvent.Types.OTP_DISPATCH, account.getUsername(), "otp requested", data);

        // channel-specific delivery (mail/sms/console). kafka relies on event bus sinks.
        if (!"kafka".equalsIgnoreCase(channel)) {
            sender.send(destination, code);
        }

        return new RequestOtpResponseDto(account.getUsername(), maskDestination(destination), ttlSeconds, expires);
    }

    @Transactional(readOnly = true)
    public Account verifyForLogin(String username, String code) {
        if (code == null || code.isBlank()) {
            throw AccountException.badRequest("code required");
        }
        Account account = requireAccount(username);
        OtpCodeStore.Entry entry = store.get(account.getUsername());
        if (entry == null || !entry.code().equals(code.trim())) {
            throw AccountException.badRequest("invalid or expired otp");
        }
        store.remove(account.getUsername());
        if (!account.isEnabled()) {
            throw AccountException.badRequest("account disabled");
        }
        return account;
    }

    public VerifyOtpResponseDto verify(String username, String code) {
        Account account = verifyForLogin(username, code);
        return new VerifyOtpResponseDto(true, account.getUsername());
    }

    private Account requireAccount(String username) {
        return accountRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> AccountException.notFound("account not found"));
    }

    private String resolveDestination(Account account) {
        if ("sms".equalsIgnoreCase(channel) && account.getPhone() != null && !account.getPhone().isBlank()) {
            return account.getPhone();
        }
        if (account.getEmail() != null) {
            return account.getEmail();
        }
        return account.getUsername();
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, length);
        int n = random.nextInt(bound / 10, bound);
        return String.valueOf(n);
    }

    private static String maskDestination(String destination) {
        if (destination == null || !destination.contains("@")) {
            if (destination != null && destination.length() > 4) {
                return "***" + destination.substring(destination.length() - 4);
            }
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
