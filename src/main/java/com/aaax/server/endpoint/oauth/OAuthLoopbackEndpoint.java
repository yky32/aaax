package com.aaax.server.endpoint.oauth;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Loopback redirect for local PKCE ({@code http://127.0.0.1:8081/authorized}).
 * Native apps should use a system browser + PKCE + loopback (RFC 8252 §7.3);
 * this page is not claimed HTTPS / app-store callback.
 */
@Controller
public class OAuthLoopbackEndpoint {

    @GetMapping(value = "/authorized", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String authorized(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>AAAX</title></head><body>");
        html.append("<h1>AAAX</h1>");
        if (error != null && !error.isBlank()) {
            html.append("<p>error: ").append(escape(error)).append("</p>");
            if (errorDescription != null && !errorDescription.isBlank()) {
                html.append("<p>").append(escape(errorDescription)).append("</p>");
            }
        } else if (code != null && !code.isBlank()) {
            html.append("<p>authorization_code</p><pre>").append(escape(code)).append("</pre>");
            html.append("<p>Exchange at POST /oauth2/token with grant_type=authorization_code and code_verifier.</p>");
        } else {
            html.append("<p>No <code>code</code> query parameter. Complete /oauth2/authorize with PKCE first.</p>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    static String escape(String raw) {
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
