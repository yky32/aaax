package com.aaax.passkey;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.aaax.account.Account;
import com.aaax.account.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 /**
 * Passkeys (WebAuthn) — experimental in v0.4.0 (not production MFA).
 * Stores credentials and issues PublicKeyCredential options.
 * Full assertion crypto verification is not claimed yet.
 */
@Service
public class PasskeyService {

    private final PasskeyCredentialRepository credentials;
    private final AccountRepository accounts;
    private final IdentityEventBus events;
    private final String rpId;
    private final String rpName;
    private final String origin;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public PasskeyService(
            PasskeyCredentialRepository credentials,
            AccountRepository accounts,
            IdentityEventBus events,
            @Value("${aaax.passkeys.rp-id:localhost}") String rpId,
            @Value("${aaax.passkeys.rp-name:AAAX}") String rpName,
            @Value("${aaax.issuer:http://localhost:8081}") String issuer) {
        this.credentials = credentials;
        this.accounts = accounts;
        this.events = events;
        this.rpId = rpId;
        this.rpName = rpName;
        this.origin = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    }

    public Map<String, Object> registrationOptions(String username) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String challenge = randomChallenge();
        challenges.put("reg:" + username, new Challenge(challenge, username, System.currentTimeMillis() + 300_000));
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", b64Url(account.getId().getBytes()));
        user.put("name", account.getUsername());
        user.put("displayName", account.getUsername());
        Map<String, Object> rp = Map.of("id", rpId, "name", rpName);
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("challenge", challenge);
        opts.put("rp", rp);
        opts.put("user", user);
        opts.put("pubKeyCredParams", List.of(
                Map.of("type", "public-key", "alg", -7),
                Map.of("type", "public-key", "alg", -257)));
        opts.put("timeout", 120000);
        opts.put("attestation", "none");
        opts.put("authenticatorSelection", Map.of(
                "residentKey", "preferred",
                "userVerification", "preferred"));
        opts.put("excludeCredentials", credentials.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(c -> Map.of("type", "public-key", "id", c.getCredentialId()))
                .toList());
        return opts;
    }

    @Transactional
    public Map<String, Object> register(String username, RegisterRequest body) {
        Challenge ch = challenges.remove("reg:" + username);
        if (ch == null || ch.expiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registration challenge expired");
        }
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (body.credentialId() == null || body.publicKeyCoseBase64() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "credentialId and publicKeyCoseBase64 required");
        }
        if (credentials.findByCredentialId(body.credentialId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "credential already registered");
        }
        PasskeyCredential cred = new PasskeyCredential();
        cred.setId(UUID.randomUUID().toString());
        cred.setAccountId(account.getId());
        cred.setCredentialId(body.credentialId());
        cred.setPublicKeyCose(Base64.getUrlDecoder().decode(normalizeB64(body.publicKeyCoseBase64())));
        cred.setSignCount(body.signCount() == null ? 0L : body.signCount());
        cred.setLabel(body.label() != null ? body.label() : "Passkey");
        credentials.save(cred);
        events.emit("com.aaax.passkey.registered", username, Map.of("credentialId", body.credentialId()));
        return Map.of("id", cred.getId(), "label", cred.getLabel(), "createdAt", cred.getCreatedAt().toString());
    }

    public Map<String, Object> authenticationOptions(String usernameOrNull) {
        String challenge = randomChallenge();
        String key = usernameOrNull == null || usernameOrNull.isBlank()
                ? "auth:anon:" + challenge
                : "auth:" + usernameOrNull;
        challenges.put(key, new Challenge(challenge, usernameOrNull, System.currentTimeMillis() + 300_000));
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("challenge", challenge);
        opts.put("timeout", 120000);
        opts.put("rpId", rpId);
        opts.put("userVerification", "preferred");
        if (usernameOrNull != null && !usernameOrNull.isBlank()) {
            accounts.findByUsernameIgnoreCase(usernameOrNull).ifPresent(a ->
                    opts.put("allowCredentials", credentials.findByAccountIdOrderByCreatedAtDesc(a.getId()).stream()
                            .map(c -> Map.of("type", "public-key", "id", c.getCredentialId()))
                            .toList()));
        }
        opts.put("challengeKey", key);
        return opts;
    }

    @Transactional
    public Account authenticate(AuthenticateRequest body) {
        if (body.challengeKey() == null || body.credentialId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "challengeKey and credentialId required");
        }
        Challenge ch = challenges.remove(body.challengeKey());
        if (ch == null || ch.expiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "auth challenge expired");
        }
        PasskeyCredential cred = credentials.findByCredentialId(body.credentialId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unknown passkey"));
        // Phase-1: presence + challenge bind. Tighten with assertion signature verify next.
        if (body.signCount() != null && body.signCount() >= cred.getSignCount()) {
            cred.setSignCount(body.signCount());
            credentials.save(cred);
        }
        Account account = accounts.findById(cred.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        events.emit(IdentityEvent.Types.AUTH_LOGIN, account.getUsername(), "passkey",
                Map.of("method", "passkey", "credentialId", body.credentialId()));
        return account;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String username) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return credentials.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("label", c.getLabel());
                    m.put("credentialId", c.getCredentialId());
                    m.put("createdAt", c.getCreatedAt().toString());
                    return m;
                })
                .toList();
    }

    @Transactional
    public void delete(String username, String id) {
        Account account = accounts.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        credentials.deleteByIdAndAccountId(id, account.getId());
    }

    public String getOrigin() {
        return origin;
    }

    private String randomChallenge() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return b64Url(b);
    }

    private static String b64Url(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static String normalizeB64(String b64) {
        String s = b64.trim().replace('+', '-').replace('/', '_');
        int m = s.length() % 4;
        if (m > 0) {
            s = s + "====".substring(m);
        }
        return s;
    }

    private record Challenge(String value, String username, long expiresAt) {
    }

    public record RegisterRequest(String credentialId, String publicKeyCoseBase64, Long signCount, String label) {
    }

    public record AuthenticateRequest(String challengeKey, String credentialId, Long signCount, String clientDataJSON) {
    }
}
