package com.aaax.server.endpoint.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthLoopbackEndpointTest {

    private final OAuthLoopbackEndpoint endpoint = new OAuthLoopbackEndpoint();

    @Test
    void authorized_rendersEscapedCode() {
        String html = endpoint.authorized("ab<>&\"c", null, null);
        assertTrue(html.contains("ab&lt;&gt;&amp;&quot;c"));
        assertFalse(html.contains("ab<>"));
    }

    @Test
    void authorized_rendersError() {
        String html = endpoint.authorized(null, "access_denied", "nope");
        assertTrue(html.contains("access_denied"));
        assertTrue(html.contains("nope"));
    }
}
