package com.aaax.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.aaax.entity.po.account.Account;
import com.aaax.events.IdentityEvent;
import com.aaax.usecase.account.FederateAccountUseCase;
import com.aaax.usecase.auth.FinishAuthenticatedSession;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Social login → federate / link → {@link FinishAuthenticatedSession}.
 */
@Configuration
public class SocialLoginConfig {

    public static boolean anySocialEnabled(Environment env) {
        return SocialProviders.anySocialEnabled(env);
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> aaaxOAuth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        RestTemplate rest = new RestTemplate();
        rest.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        return userRequest -> {
            OAuth2User user = delegate.loadUser(userRequest);
            String reg = userRequest.getClientRegistration().getRegistrationId();
            if ("github".equalsIgnoreCase(reg)) {
                return enrichGithubEmail(userRequest, user, rest);
            }
            return user;
        };
    }

    @Bean(name = "socialLoginSuccessHandler")
    AuthenticationSuccessHandler socialLoginSuccessHandler(
            FederateAccountUseCase federateAccountUseCase, FinishAuthenticatedSession finishAuthenticatedSession) {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request, HttpServletResponse response, Authentication authentication)
                    throws IOException, ServletException {
                if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                    response.sendRedirect("/sign-in/?error=social");
                    return;
                }
                HttpSession session = request.getSession(false);
                String linkUser =
                        session == null ? null : (String) session.getAttribute(SocialProviders.SESSION_LINK_USER);
                String returnTo =
                        session == null ? null : (String) session.getAttribute(SocialProviders.SESSION_RETURN);
                if (session != null) {
                    session.removeAttribute(SocialProviders.SESSION_LINK_USER);
                    session.removeAttribute(SocialProviders.SESSION_RETURN);
                }

                String provider = token.getAuthorizedClientRegistrationId();
                Account account;
                try {
                    ProviderIdentity id = extract(provider, token.getPrincipal());
                    if (StringUtils.hasText(linkUser)) {
                        account = federateAccountUseCase.linkToUsername(linkUser, provider, id.externalId());
                    } else {
                        account = federateAccountUseCase.linkOrCreate(
                                provider, id.externalId(), id.email(), id.nameHint());
                    }
                } catch (Exception ex) {
                    response.sendRedirect("/sign-in/?error=" + encode(ex.getMessage()));
                    return;
                }

                finishAuthenticatedSession.execute(
                        account,
                        "social",
                        request,
                        response,
                        IdentityEvent.Types.AUTH_LOGIN_SOCIAL,
                        Map.of(
                                "method",
                                "social",
                                "provider",
                                provider,
                                "linked",
                                StringUtils.hasText(linkUser)));

                response.sendRedirect(safeReturn(returnTo, account));
            }
        };
    }

    @Bean(name = "socialLoginFailureHandler")
    AuthenticationFailureHandler socialLoginFailureHandler() {
        SimpleUrlAuthenticationFailureHandler h =
                new SimpleUrlAuthenticationFailureHandler("/sign-in/?error=social_failed");
        h.setUseForward(false);
        return h;
    }

    static ProviderIdentity extract(String provider, OAuth2User user) {
        String p = SocialProviders.normalize(provider);
        if (user instanceof OidcUser oidc) {
            String sub = oidc.getSubject();
            String email = oidc.getEmail();
            String name = oidc.getFullName() != null ? oidc.getFullName() : oidc.getGivenName();
            if (!StringUtils.hasText(name)) {
                name = oidc.getPreferredUsername();
            }
            if ("apple".equals(p) || "google".equals(p) || "line".equals(p) || "slack".equals(p)) {
                return new ProviderIdentity(sub, email, name != null ? name : email);
            }
        }
        return switch (p) {
            case "github" -> {
                Object id = user.getAttribute("id");
                String ext = id == null ? user.getName() : String.valueOf(id);
                String email = attr(user, "email");
                String login = attr(user, "login");
                if (!StringUtils.hasText(email)) {
                    email = (login != null ? login : ext) + "@users.noreply.github.com";
                }
                yield new ProviderIdentity(ext, email, login != null ? login : ext);
            }
            case "discord" -> {
                String ext = firstNonBlank(attr(user, "id"), user.getName());
                String email = attr(user, "email");
                String name = firstNonBlank(attr(user, "global_name"), attr(user, "username"), ext);
                yield new ProviderIdentity(ext, email, name);
            }
            case "gitlab" -> {
                String ext = firstNonBlank(stringAttr(user, "id"), user.getName());
                String email = attr(user, "email");
                String name = firstNonBlank(attr(user, "username"), attr(user, "name"), ext);
                yield new ProviderIdentity(ext, email, name);
            }
            case "line" -> {
                String ext = firstNonBlank(attr(user, "userId"), attr(user, "sub"), user.getName());
                String email = attr(user, "email");
                String name = firstNonBlank(attr(user, "displayName"), attr(user, "name"), ext);
                yield new ProviderIdentity(ext, email, name);
            }
            case "slack" -> {
                String ext = firstNonBlank(attr(user, "sub"), attr(user, "https://slack.com/user_id"), user.getName());
                String email = attr(user, "email");
                String name = firstNonBlank(attr(user, "name"), attr(user, "https://slack.com/username"), ext);
                yield new ProviderIdentity(ext, email, name);
            }
            case "apple" -> {
                String ext = firstNonBlank(attr(user, "sub"), user.getName());
                String email = attr(user, "email");
                yield new ProviderIdentity(ext, email, email != null ? email : "apple");
            }
            default -> {
                String ext = firstNonBlank(attr(user, "sub"), attr(user, "id"), user.getName());
                yield new ProviderIdentity(ext, attr(user, "email"), ext);
            }
        };
    }

    private static OAuth2User enrichGithubEmail(OAuth2UserRequest userRequest, OAuth2User user, RestTemplate rest) {
        Object emailAttr = user.getAttribute("email");
        if (emailAttr != null && StringUtils.hasText(String.valueOf(emailAttr))) {
            return user;
        }
        try {
            String token = userRequest.getAccessToken().getTokenValue();
            RequestEntity<Void> req = RequestEntity.get(
                            UriComponentsBuilder.fromUriString("https://api.github.com/user/emails").build().toUri())
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .build();
            @SuppressWarnings("rawtypes")
            ResponseEntity<List> resp = rest.exchange(req, List.class);
            List<?> body = resp.getBody();
            String primary = null;
            if (body != null) {
                for (Object row : body) {
                    if (!(row instanceof Map<?, ?> m)) {
                        continue;
                    }
                    Object em = m.get("email");
                    Object prim = m.get("primary");
                    Object ver = m.get("verified");
                    if (em == null) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(prim) && (ver == null || Boolean.TRUE.equals(ver))) {
                        primary = String.valueOf(em);
                        break;
                    }
                    if (primary == null && (ver == null || Boolean.TRUE.equals(ver))) {
                        primary = String.valueOf(em);
                    }
                }
            }
            if (!StringUtils.hasText(primary)) {
                return user;
            }
            Map<String, Object> attrs = new java.util.LinkedHashMap<>(user.getAttributes());
            attrs.put("email", primary);
            String nameAttr = userRequest
                    .getClientRegistration()
                    .getProviderDetails()
                    .getUserInfoEndpoint()
                    .getUserNameAttributeName();
            return new DefaultOAuth2User(user.getAuthorities(), attrs, nameAttr);
        } catch (Exception ex) {
            return user;
        }
    }

    private static String attr(OAuth2User user, String key) {
        Object v = user.getAttribute(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String stringAttr(OAuth2User user, String key) {
        Object v = user.getAttribute(key);
        return v == null ? null : String.valueOf(v);
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return null;
    }

    private static String safeReturn(String returnTo, Account account) {
        if (StringUtils.hasText(returnTo)
                && returnTo.startsWith("/")
                && !returnTo.startsWith("//")
                && !returnTo.contains("://")) {
            return returnTo;
        }
        return account.roleSet().contains("ADMIN") ? "/admin/" : "/user/";
    }

    private static String encode(String msg) {
        if (msg == null) {
            return "social";
        }
        String m = msg.length() > 80 ? msg.substring(0, 80) : msg;
        return java.net.URLEncoder.encode(m, java.nio.charset.StandardCharsets.UTF_8);
    }

    record ProviderIdentity(String externalId, String email, String nameHint) {}
}
