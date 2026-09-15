package com.aaax.server.endpoint.oauth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hosted HTML login for {@code /oauth2/authorize} (saved-request resume after POST /login).
 */
@Controller
public class HostedLoginEndpoint {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
