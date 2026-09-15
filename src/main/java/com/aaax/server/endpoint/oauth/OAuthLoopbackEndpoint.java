package com.aaax.server.endpoint.oauth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Loopback redirect for local PKCE ({@code http://127.0.0.1:8081/authorized}).
 * Native apps should use a system browser + PKCE + loopback (RFC 8252 §7.3);
 * this page is not claimed HTTPS / app-store callback.
 */
@Controller
public class OAuthLoopbackEndpoint {

    @GetMapping("/authorized")
    public String authorized(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            Model model) {
        model.addAttribute("code", code);
        model.addAttribute("error", error);
        model.addAttribute("errorDescription", errorDescription);
        return "authorized";
    }
}
