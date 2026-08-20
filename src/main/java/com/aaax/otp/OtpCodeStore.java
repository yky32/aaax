package com.aaax.otp;

import java.time.Instant;

/**
 * Pluggable OTP code storage. Default: in-memory (single node).
 * Multi-node: {@code aaax.otp.store=redis}.
 */
public interface OtpCodeStore {

    void put(String key, String code, Instant expiresAt);

    /** @return entry or null if missing/expired */
    Entry get(String key);

    void remove(String key);

    record Entry(String code, Instant expiresAt) {
    }
}
