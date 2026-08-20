package com.aaax.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public list of configured social login providers for the admin console.
 */
@RestController
@RequestMapping("/v1/auth/social")
public class SocialAuthEndpoint {

    private final Environment env;

    public SocialAuthEndpoint(Environment env) {
        this.env = env;
    }

    @GetMapping("/providers")
    public Map<String, Object> providers() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (has("spring.security.oauth2.client.registration.google.client-id")) {
            list.add(provider("google", "Google", "/oauth2/authorization/google"));
        }
        if (has("spring.security.oauth2.client.registration.github.client-id")) {
            list.add(provider("github", "GitHub", "/oauth2/authorization/github"));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providers", list);
        body.put("enabled", !list.isEmpty());
        return body;
    }

    private Map<String, Object> provider(String id, String label, String authorizationUrl) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("authorizationUrl", authorizationUrl);
        return m;
    }

    private boolean has(String key) {
        return StringUtils.hasText(env.getProperty(key));
    }
}
