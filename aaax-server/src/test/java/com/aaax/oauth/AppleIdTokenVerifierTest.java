package com.aaax.oauth;

import com.aaax.core.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AppleIdTokenVerifierTest {

    @Test
    @DisplayName("verify should reject blank idToken")
    void verify_shouldRejectBlank() {
        AppleIdTokenVerifier verifier = new AppleIdTokenVerifier("ios.client", "web.client");
        assertThrows(BizException.class, () -> verifier.verify(""));
        assertThrows(BizException.class, () -> verifier.verify(null));
    }

    @Test
    @DisplayName("verify should reject when no client ids configured")
    void verify_shouldRejectMissingAudienceConfig() {
        AppleIdTokenVerifier verifier = new AppleIdTokenVerifier("", "");
        assertThrows(BizException.class, () -> verifier.verify("header.payload.sig"));
    }

    @Test
    @DisplayName("verify should wrap invalid JWT parse failures")
    void verify_shouldWrapInvalidJwt() {
        AppleIdTokenVerifier verifier = new AppleIdTokenVerifier("ios.client", "web.client");
        assertThrows(BizException.class, () -> verifier.verify("not-a-jwt"));
    }
}
