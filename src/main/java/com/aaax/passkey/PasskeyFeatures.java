package com.aaax.passkey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Passkeys are <b>off by default</b>. When enabled, registration/assertion use webauthn4j.
 * Enable: {@code aaax.passkeys.enabled=true} or {@code AAAX_PASSKEYS_ENABLED=true}.
 */
@Component
public class PasskeyFeatures {

    private final boolean enabled;

    public PasskeyFeatures(@Value("${aaax.passkeys.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "passkeys disabled (set aaax.passkeys.enabled=true; webauthn4j verify when on — see docs/PASSKEYS.md)");
        }
    }
}
