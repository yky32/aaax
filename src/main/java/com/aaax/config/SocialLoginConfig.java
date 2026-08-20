package com.aaax.config;

import java.io.IOException;
import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.AccountService;
import com.aaax.account.AccountUserDetailsService;
import com.aaax.events.IdentityEvent;
import com.aaax.events.IdentityEventBus;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.util.StringUtils;

/**
 * Social login success → link/create Account → session UserDetails → event bus.
 * Enabled when any social client-id is set (google and/or github).
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

    @Bean
    AuthenticationSuccessHandler socialLoginSuccessHandler(
            AccountService accountService,
            AccountUserDetailsService userDetailsService,
            IdentityEventBus events) {
        SecurityContextRepository repo = new HttpSessionSecurityContextRepository();
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                String provider = "unknown";
                Account account;
                if (authentication instanceof OAuth2AuthenticationToken token) {
                    provider = token.getAuthorizedClientRegistrationId();
                    OAuth2User principal = token.getPrincipal();
                    account = resolveAccount(provider, principal, accountService);
                } else {
                    response.sendRedirect("/admin/?error=social");
                    return;
                }

                UserDetails details = userDetailsService.loadUserByUsername(account.getUsername());
                UsernamePasswordAuthenticationToken sessionAuth =
                        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(sessionAuth);
                SecurityContextHolder.setContext(context);
                repo.saveContext(context, request, response);

                events.emit(
                        IdentityEvent.Types.AUTH_LOGIN_SOCIAL,
                        account.getUsername(),
                        "social:" + provider,
                        Map.of("method", "social", "provider", provider));

                String target = "/admin/";
                if (!account.roleSet().contains("ADMIN")) {
                    // non-admin social users land on root meta JSON-friendly page
                    target = "/?social=ok";
                }
                response.sendRedirect(target);
            }
        };
    }

    static Account resolveAccount(String provider, OAuth2User user, AccountService accounts) {
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
                // public email may be null; use noreply placeholder unique per github id
                email = githubId + "+github@users.noreply.github.com";
            }
            return accounts.linkOrCreateGithub(githubId, email, login);
        }
        // generic OIDC-ish fallback
        String sub = user.getName();
        String email = user.getAttribute("email");
        return accounts.linkOrCreateGoogle(provider + ":" + sub, email, sub);
    }
}
