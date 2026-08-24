package com.aaax.endpoint.auth;

import java.util.Map;

import com.aaax.config.SocialProviders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public list of configured social login providers (sign-in + admin + account link UI).
 */
@RestController
@RequestMapping("/v1/auth/social")
public class SocialAuthEndpoint {

    private final SocialProviders socialProviders;

    public SocialAuthEndpoint(SocialProviders socialProviders) {
        this.socialProviders = socialProviders;
    }

    @GetMapping("/providers")
    public Map<String, Object> providers() {
        return socialProviders.toPublicBody();
    }
}
