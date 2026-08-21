package com.aaax.auth;

import java.util.Optional;

/** Pluggable QR login sessions. Default memory; Redis when {@code aaax.qr.store=redis}. */
public interface QrLoginSessionStore {

    int ttlSeconds();

    QrLoginSession create();

    Optional<QrLoginSession> get(String id);

    Optional<QrLoginSession> getByUserCode(String code);

    /** Persist mutations (status / approvedUsername). No-op for pure memory maps if already shared. */
    void save(QrLoginSession session);

    void remove(String id);
}
