package com.aaax.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.aaax.account.Account;
import com.aaax.account.AccountException;
import com.aaax.account.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.otp.OtpSender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Magic link (Clerk-style passwordless email link).
 * Token is single-use; delivery via existing {@link OtpSender} channel (console/mail/kafka/sms).
 */
@Service
public class MagicLinkService {

    private final AccountRepository accounts;
    private final OtpSender otpSender;
    private final IdentityEventBus events;
    private final String issuer;
    private final int ttlSeconds;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    public MagicLinkService(
            AccountRepository accounts,
            OtpSender otpSender,
            IdentityEventBus events,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.magic.ttl-seconds:900}") int ttlSeconds) {
        this.accounts = accounts;
        this.otpSender = otpSender;
        this.events = events;
        this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public Map<String, Object> request(String usernameOrEmail) {
        Account account = accounts.findByUsernameIgnoreCase(usernameOrEmail.trim())
                .or(() -> accounts.findByEmailIgnoreCase(usernameOrEmail.trim()))
                .orElseThrow(() -> new AccountException(org.springframework.http.HttpStatus.NOT_FOUND, "account not found"));
        String token = newToken();
        Instant exp = Instant.now().plusSeconds(ttlSeconds);
        tokens.put(token, new Entry(account.getUsername(), exp));
        String link = issuer + "/sign-in/#magic=" + token;
        String dest = account.getEmail() != null && !account.getEmail().isBlank()
                ? account.getEmail()
                : account.getUsername();
        otpSender.send(dest, "MAGIC:" + link);
        events.emit(IdentityEvent.Types.OTP_DISPATCH, account.getUsername(),
                Map.of("channel", "magic_link", "purpose", "magic_link", "destination", dest));
        return Map.of(
                "sent", true,
                "expiresInSeconds", ttlSeconds,
                // dev convenience when channel=console — same token is in logs/link
                "devLink", link);
    }

    public Optional<String> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Entry e = tokens.remove(token.trim());
        if (e == null || Instant.now().isAfter(e.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(e.username());
    }

    private String newToken() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private record Entry(String username, Instant expiresAt) {
    }
}
