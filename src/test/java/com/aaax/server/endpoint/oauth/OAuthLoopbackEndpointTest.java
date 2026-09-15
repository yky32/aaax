package com.aaax.server.endpoint.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OAuthLoopbackEndpointTest {

    private final OAuthLoopbackEndpoint endpoint = new OAuthLoopbackEndpoint();

    @Test
    @DisplayName("authorized should pass code into model and resolve authorized view")
    void authorized_shouldPassCodeToModel() {
        Model model = new ConcurrentModel();
        assertEquals("authorized", endpoint.authorized("auth-code-1", null, null, model));
        assertEquals("auth-code-1", model.getAttribute("code"));
        assertNull(model.getAttribute("error"));
    }

    @Test
    @DisplayName("authorized should pass OAuth error params into model")
    void authorized_shouldPassErrorToModel() {
        Model model = new ConcurrentModel();
        assertEquals("authorized", endpoint.authorized(null, "access_denied", "nope", model));
        assertEquals("access_denied", model.getAttribute("error"));
        assertEquals("nope", model.getAttribute("errorDescription"));
    }
}
