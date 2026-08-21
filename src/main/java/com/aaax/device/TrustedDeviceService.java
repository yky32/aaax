package com.aaax.device;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import com.aaax.account.Account;
import com.aaax.core.id.Ids;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Policy "remember this device" — cookie + hashed token store.
 * When present and valid, password login may skip TOTP MFA.
 */
@Service
public class TrustedDeviceService {

    public static final String COOKIE_NAME = "AAAX_DEVICE";

    private final TrustedDeviceRepository repository;
    private final IdentityEventBus events;
    private final int ttlDays;
    private final boolean cookieSecure;
    private final SecureRandom random = new SecureRandom();

    public TrustedDeviceService(
            TrustedDeviceRepository repository,
            IdentityEventBus events,
            @Value("${aaax.devices.ttl-days:30}") int ttlDays,
            @Value("${aaax.devices.cookie-secure:false}") boolean cookieSecure) {
        this.repository = repository;
        this.events = events;
        this.ttlDays = ttlDays;
        this.cookieSecure = cookieSecure;
    }

    public Optional<String> readRawToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public boolean isTrusted(String accountId, String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return repository.findByTokenHashAndRevokedAtIsNull(hash(rawToken))
                .filter(TrustedDevice::isActive)
                .filter(d -> d.getAccountId().equals(accountId))
                .isPresent();
    }

    @Transactional
    public void touch(String accountId, String rawToken, HttpServletRequest request) {
        repository.findByTokenHashAndRevokedAtIsNull(hash(rawToken))
                .filter(TrustedDevice::isActive)
                .filter(d -> d.getAccountId().equals(accountId))
                .ifPresent(d -> {
                    d.setLastSeenAt(Instant.now());
                    if (request != null) {
                        d.setIp(clientIp(request));
                        String ua = request.getHeader("User-Agent");
                        if (ua != null) {
                            d.setUserAgent(ua.length() > 512 ? ua.substring(0, 512) : ua);
                        }
                    }
                    repository.save(d);
                });
    }

    /**
     * Create device row + set HttpOnly cookie. Returns public metadata (not raw token).
     */
    @Transactional
    public TrustedDevice registerAndSetCookie(
            Account account,
            String label,
            HttpServletRequest request,
            HttpServletResponse response) {
        String raw = newToken();
        TrustedDevice d = new TrustedDevice();
        d.setId(Ids.uuid());
        d.setAccountId(account.getId());
        d.setTokenHash(hash(raw));
        d.setLabel(label != null && !label.isBlank() ? label : guessLabel(request));
        d.setUserAgent(ua(request));
        d.setIp(clientIp(request));
        d.setExpiresAt(Instant.now().plusSeconds(ttlDays * 86400L));
        repository.save(d);
        writeCookie(response, raw);
        events.emit(
                IdentityEvent.Types.DEVICE_TRUSTED,
                account.getUsername(),
                Mapish(d));
        return d;
    }

    @Transactional(readOnly = true)
    public List<TrustedDevice> listActive(String accountId) {
        return repository.findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(accountId).stream()
                .filter(TrustedDevice::isActive)
                .toList();
    }

    @Transactional
    public void revoke(String accountId, String deviceId) {
        repository.findById(deviceId).ifPresent(d -> {
            if (d.getAccountId().equals(accountId) && d.getRevokedAt() == null) {
                d.setRevokedAt(Instant.now());
                repository.save(d);
            }
        });
    }

    @Transactional
    public void revokeAll(String accountId) {
        for (TrustedDevice d : repository.findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(accountId)) {
            d.setRevokedAt(Instant.now());
            repository.save(d);
        }
    }

    public void clearCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void writeCookie(HttpServletResponse response, String raw) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, raw)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(ttlDays * 86400L)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String newToken() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String ua(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null) {
            return null;
        }
        return ua.length() > 512 ? ua.substring(0, 512) : ua;
    }

    private static String guessLabel(HttpServletRequest request) {
        String ua = ua(request);
        if (ua == null) {
            return "Trusted device";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "Apple device";
        }
        if (ua.contains("Android")) {
            return "Android device";
        }
        if (ua.contains("Mac")) {
            return "Mac";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        return "Browser";
    }

    private static java.util.Map<String, Object> Mapish(TrustedDevice d) {
        return java.util.Map.of(
                "deviceId", d.getId(),
                "label", d.getLabel() != null ? d.getLabel() : "",
                "expiresAt", d.getExpiresAt().toString());
    }
}
