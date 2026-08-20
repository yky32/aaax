package com.aaax.auth;

import java.time.Instant;
import java.util.Optional;

/** Pluggable magic-link token store. Default memory; Redis when {@code aaax.otp.store=redis}. */
public interface MagicLinkTokenStore {

    void put(String token, String username, Instant expiresAt);

    /** Atomically get+delete; empty if missing/expired. */
    Optional<String> consume(String token);
}
