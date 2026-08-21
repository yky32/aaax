package com.aaax.usecase.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import com.aaax.entity.po.Account;
import com.aaax.exception.AccountException;
import com.aaax.repository.AccountRepository;
import com.aaax.usecase.account.AccountQueries;
import com.aaax.spi.auth.MagicLinkTokenStore;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;
import com.aaax.spi.otp.OtpSender;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Magic link request + consume (token store pluggable: memory | redis). */
@Component
public class MagicLinkUseCase {

    private final AccountRepository accounts;
    private final AccountQueries queries;
    private final OtpSender otpSender;
    private final IdentityEventBus events;
    private final FinishAuthenticatedSession finish;
    private final MagicLinkTokenStore tokenStore;
    private final String issuer;
    private final int ttlSeconds;
    private final SecureRandom random = new SecureRandom();

    public MagicLinkUseCase(
            AccountRepository accounts,
            AccountQueries queries,
            OtpSender otpSender,
            IdentityEventBus events,
            FinishAuthenticatedSession finish,
            MagicLinkTokenStore tokenStore,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer,
            @Value("${aaax.magic.ttl-seconds:900}") int ttlSeconds) {
        this.accounts = accounts;
        this.queries = queries;
        this.otpSender = otpSender;
        this.events = events;
        this.finish = finish;
        this.tokenStore = tokenStore;
        this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
        this.ttlSeconds = ttlSeconds;
    }

    public Map<String, Object> request(RequestCommand cmd) {
        String usernameOrEmail = cmd.identifier().trim();
        Account account = accounts.findByUsernameIgnoreCase(usernameOrEmail)
                .or(() -> accounts.findByEmailIgnoreCase(usernameOrEmail))
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "account not found"));
        String token = newToken();
        tokenStore.put(token, account.getUsername(), Instant.now().plusSeconds(ttlSeconds));
        String link = issuer + "/sign-in/#magic=" + token;
        String dest = account.getEmail() != null && !account.getEmail().isBlank()
                ? account.getEmail()
                : account.getUsername();
        otpSender.send(dest, "MAGIC:" + link);
        events.emit(
                IdentityEvent.Types.OTP_DISPATCH,
                account.getUsername(),
                Map.of("channel", "magic_link", "purpose", "magic_link", "destination", dest));
        return Map.of(
                "sent", true,
                "expiresInSeconds", ttlSeconds,
                "devLink", link);
    }

    public Map<String, Object> consume(
            ConsumeCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        String username = tokenStore.consume(cmd.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or expired magic link"));
        return finish.execute(queries.requireEntityByUsername(username), "magic_link", request, response, true);
    }

    private String newToken() {
        byte[] b = new byte[24];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    public record RequestCommand(@NotBlank @Size(max = 128) String identifier) {
    }

    public record ConsumeCommand(@NotBlank @Size(max = 128) String token) {
    }
}
