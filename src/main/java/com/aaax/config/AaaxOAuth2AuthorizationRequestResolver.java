package com.aaax.config;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Captures {@code aaax_return} and {@code aaax_link=1} on {@code /oauth2/authorization/{id}}.
 * No-op when no OAuth2 client registrations are configured.
 */
@Component
public class AaaxOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public AaaxOAuth2AuthorizationRequestResolver(ObjectProvider<ClientRegistrationRepository> clients) {
        ClientRegistrationRepository repo = clients.getIfAvailable();
        if (repo != null) {
            this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                    repo, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        } else {
            this.delegate = null;
        }
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        if (delegate == null) {
            return null;
        }
        capture(request);
        return customize(delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        if (delegate == null) {
            return null;
        }
        capture(request);
        return customize(delegate.resolve(request, clientRegistrationId));
    }

    private void capture(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String ret = request.getParameter("aaax_return");
        if (StringUtils.hasText(ret) && ret.startsWith("/") && !ret.startsWith("//")) {
            session.setAttribute(SocialProviders.SESSION_RETURN, ret);
        }
        if ("1".equals(request.getParameter("aaax_link"))) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null
                    && auth.isAuthenticated()
                    && auth.getName() != null
                    && !"anonymousUser".equals(auth.getName())) {
                session.setAttribute(SocialProviders.SESSION_LINK_USER, auth.getName());
            }
        }
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
        if (req == null) {
            return null;
        }
        Map<String, Object> extra = new HashMap<>(req.getAdditionalParameters());
        return OAuth2AuthorizationRequest.from(req).additionalParameters(extra).build();
    }
}
