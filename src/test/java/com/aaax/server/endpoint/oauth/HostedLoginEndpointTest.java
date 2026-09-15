package com.aaax.server.endpoint.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HostedLoginEndpointTest {

    private final HostedLoginEndpoint endpoint = new HostedLoginEndpoint();

    @Test
    @DisplayName("GET /login should resolve Thymeleaf login view")
    void login_shouldResolveLoginView() {
        assertEquals("login", endpoint.login());
    }
}
