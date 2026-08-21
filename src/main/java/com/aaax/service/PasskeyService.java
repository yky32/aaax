package com.aaax.service;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.aaax.entity.po.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.aaax.entity.po.PasskeyCredential;
import com.aaax.repository.PasskeyCredentialRepository;

/**
 * Passkeys (WebAuthn) with <b>webauthn4j</b> attestation/assertion verify.
 * Still gated by {@code aaax.passkeys.enabled} (default false).
 */
@Service
public class PasskeyService {

    private final PasskeyCredentialRepository credentials;
    private final AccountRepository accounts;
    private final IdentityEventBus events;
    private final String rpId;
    private final String rpName;
    private final String origin;
    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final ObjectConverter objectConverter = new ObjectConverter();
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
        byte[] challengeBytes = new DefaultChallenge().getValue();
        String challenge = b64Url(challengeBytes);
        challenges.put("reg:" + username, new Challenge(challengeBytes, username, System.currentTimeMillis() + 300_000));
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", b64Url(account.getId().getBytes()));
        user.put("name", account.getUsername());
        user.put("displayName", account.getUsername());
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("challenge", challenge);
        opts.put("rp", Map.of("id", rpId, "name", rpName));
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
        if (body.clientDataJSON() == null || body.attestationObject() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "clientDataJSON and attestationObject required (WebAuthn registration response)");
        }
        try {
            byte[] clientDataJSON = decode(body.clientDataJSON());
            byte[] attestationObject = decode(body.attestationObject());
            ServerProperty serverProperty = new ServerProperty(
                    new Origin(origin), rpId, new DefaultChallenge(ch.value()));
            RegistrationRequest regReq = new RegistrationRequest(attestationObject, clientDataJSON);
            // userVerificationRequired=false, userPresenceRequired=true
            RegistrationParameters params = new RegistrationParameters(serverProperty, null, false, true);
            RegistrationData regData = webAuthnManager.verify(regReq, params);
            AttestedCredentialData acd = regData.getAttestationObject()
                    .getAuthenticatorData()
                    .getAttestedCredentialData();
            if (acd == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no attested credential data");
            }
            String credentialId = b64Url(acd.getCredentialId());
            if (credentials.findByCredentialId(credentialId).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "credential already registered");
            }
            long counter = regData.getAttestationObject().getAuthenticatorData().getSignCount();
            byte[] publicKeyCose = objectConverter.getCborConverter().writeValueAsBytes(acd.getCOSEKey());
            byte[] aaguid = acd.getAaguid() != null ? acd.getAaguid().getBytes() : AAGUID.ZERO.getBytes();

            PasskeyCredential cred = new PasskeyCredential();
            cred.setId(UUID.randomUUID().toString());
            cred.setAccountId(account.getId());
            cred.setCredentialId(credentialId);
            cred.setPublicKeyCose(publicKeyCose);
            cred.setAaguid(aaguid);
            cred.setSignCount(counter);
            cred.setLabel(body.label() != null ? body.label() : "Passkey");
            credentials.save(cred);
            events.emit("com.aaax.passkey.registered", username, Map.of("credentialId", credentialId));
            return Map.of("id", cred.getId(), "label", cred.getLabel(), "createdAt", cred.getCreatedAt().toString());
        } catch (VerificationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passkey registration verify failed: " + e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passkey registration failed: " + e.getMessage());
        }
    }

    public Map<String, Object> authenticationOptions(String usernameOrNull) {
        byte[] challengeBytes = new DefaultChallenge().getValue();
        String challenge = b64Url(challengeBytes);
        String key = usernameOrNull == null || usernameOrNull.isBlank()
                ? "auth:anon:" + challenge
                : "auth:" + usernameOrNull;
        challenges.put(key, new Challenge(challengeBytes, usernameOrNull, System.currentTimeMillis() + 300_000));
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
        if (body.challengeKey() == null || body.credentialId() == null
                || body.authenticatorData() == null || body.clientDataJSON() == null || body.signature() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "challengeKey, credentialId, authenticatorData, clientDataJSON, signature required");
        }
        Challenge ch = challenges.remove(body.challengeKey());
        if (ch == null || ch.expiresAt() < System.currentTimeMillis()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "auth challenge expired");
        }
        PasskeyCredential cred = credentials.findByCredentialId(body.credentialId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unknown passkey"));
        try {
            byte[] credentialId = decode(body.credentialId());
            byte[] authenticatorData = decode(body.authenticatorData());
            byte[] clientDataJSON = decode(body.clientDataJSON());
            byte[] signature = decode(body.signature());
            byte[] userHandle = body.userHandle() != null && !body.userHandle().isBlank()
                    ? decode(body.userHandle()) : null;

            COSEKey coseKey = objectConverter.getCborConverter().readValue(cred.getPublicKeyCose(), COSEKey.class);
            AAGUID aaguid = cred.getAaguid() != null ? new AAGUID(cred.getAaguid()) : AAGUID.ZERO;
            AttestedCredentialData acd = new AttestedCredentialData(aaguid, credentialId, coseKey);
            Authenticator authenticator = new AuthenticatorImpl(acd, null, cred.getSignCount());

            ServerProperty serverProperty = new ServerProperty(
                    new Origin(origin), rpId, new DefaultChallenge(ch.value()));
            AuthenticationRequest authReq = userHandle != null
                    ? new AuthenticationRequest(credentialId, userHandle, authenticatorData, clientDataJSON, signature)
                    : new AuthenticationRequest(credentialId, authenticatorData, clientDataJSON, signature);
            AuthenticationParameters params = new AuthenticationParameters(
                    serverProperty, authenticator, null, false, true);
            AuthenticationData authData = webAuthnManager.verify(authReq, params);
            long newCount = authData.getAuthenticatorData().getSignCount();
            if (newCount > 0) {
                cred.setSignCount(newCount);
                credentials.save(cred);
            }
            Account account = accounts.findById(cred.getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            events.emit(IdentityEvent.Types.AUTH_LOGIN, account.getUsername(), "passkey",
                    Map.of("method", "passkey", "credentialId", body.credentialId(), "verified", true));
            return account;
        } catch (VerificationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "passkey assertion verify failed: " + e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "passkey auth failed: " + e.getMessage());
        }
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

    private static byte[] decode(String b64url) {
        String s = b64url.trim().replace('+', '-').replace('/', '_');
        int m = s.length() % 4;
        if (m > 0) {
            s = s + "====".substring(m);
        }
        return Base64.getUrlDecoder().decode(s);
    }

    private static String b64Url(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private record Challenge(byte[] value, String username, long expiresAt) {
    }

    /** WebAuthn registration response fields (base64url). */
    public record RegisterRequest(
            String clientDataJSON,
            String attestationObject,
            String label,
            String credentialId,
            String publicKeyCoseBase64,
            Long signCount) {
    }

    /** WebAuthn authentication assertion (base64url). */
    public record AuthenticateRequest(
            String challengeKey,
            String credentialId,
            String authenticatorData,
            String clientDataJSON,
            String signature,
            String userHandle,
            Long signCount) {
    }
}