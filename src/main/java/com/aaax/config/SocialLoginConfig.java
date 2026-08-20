package com.aaax.config;

import java.io.IOException;
import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.application.FederateAccountUseCase;
import com.aaax.auth.application.FinishAuthenticatedSession;
import com.aaax.events.IdentityEvent;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

/**
 * Social login success → federate Account → {@link FinishAuthenticatedSession}.
 */
@Configuration
public class SocialLoginConfig {

    public static boolean anySocialEnabled(Environment env) {
        return has(env, "spring.security.oauth2.client.registration.google.client-id")
                || has(env, "spring.security.oauth2.client.registration.github.client-id");
    }

    private static boolean has(Environment env, String key) {
        String v = env.getProperty(key);
        return v != null && !v.isBlank();
    }

    @Bean(name = "socialLoginSuccessHandler")
    AuthenticationSuccessHandler socialLoginSuccessHandler(
            FederateAccountUseCase federate, FinishAuthenticatedSession finishSession) {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                    response.sendRedirect("/admin/?error=social");
                    return;
                }
                String provider = token.getAuthorizedClientRegistrationId();
                Account account = resolveAccount(provider, token.getPrincipal(), federate);
                finishSession.execute(
                        account,
                        "social",
                        request,
                        response,
                        IdentityEvent.Types.AUTH_LOGIN_SOCIAL,
                        Map.of("method", "social", "provider", provider));

                String target = account.roleSet().contains("ADMIN") ? "/admin/" : "/user/";
                response.sendRedirect(target);
            }
        };
    }

    static Account resolveAccount(String provider, OAuth2User user, FederateAccountUseCase accounts) {
        if ("google".equalsIgnoreCase(provider) && user instanceof OidcUser oidc) {
            String sub = oidc.getSubject();
            String email = oidc.getEmail();
            String name = oidc.getFullName() != null ? oidc.getFullName() : oidc.getGivenName();
            return accounts.linkOrCreateGoogle(sub, email, name != null ? name : email);
        }
        if ("github".equalsIgnoreCase(provider)) {
            Object id = user.getAttribute("id");
            String githubId = id == null ? user.getName() : String.valueOf(id);
            String email = user.getAttribute("email");
            String login = user.getAttribute("login");
            if (!StringUtils.hasText(email)) {
                email = githubId + "+github@users.noreply.github.com";
            }
            return accounts.linkOrCreateGithub(githubId, email, login);
        }
        String sub = user.getName();
        String email = user.getAttribute("email");
        return accounts.linkOrCreateGoogle(provider + ":" + sub, email, sub);
    }
}
