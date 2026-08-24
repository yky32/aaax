package com.aaax.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Social provider discovery. Enabled when registration client-id env is non-blank.
 */
@Component
public class SocialProviders {

    public static final String SESSION_RETURN = "AAAX_SOCIAL_RETURN";
    public static final String SESSION_LINK_USER = "AAAX_SOCIAL_LINK_USER";

    /** Stable catalog — ids match Spring registration ids. */
    public static final List<ProviderDef> CATALOG = List.of(
            new ProviderDef("google", "Google", "OpenID Connect", List.of("openid", "profile", "email"), "GOOGLE_CLIENT_ID"),
            new ProviderDef("github", "GitHub", "OAuth 2.0", List.of("read:user", "user:email"), "GITHUB_CLIENT_ID"),
            new ProviderDef("apple", "Apple", "OpenID Connect", List.of("openid", "name", "email"), "APPLE_CLIENT_ID"),
            new ProviderDef("discord", "Discord", "OAuth 2.0", List.of("identify", "email"), "DISCORD_CLIENT_ID"),
            new ProviderDef("gitlab", "GitLab", "OAuth 2.0", List.of("read_user"), "GITLAB_CLIENT_ID"),
            new ProviderDef("line", "LINE", "OpenID Connect", List.of("profile", "openid", "email"), "LINE_CHANNEL_ID"),
            new ProviderDef("slack", "Slack", "OpenID Connect", List.of("openid", "profile", "email"), "SLACK_CLIENT_ID"));

    public static final Set<String> KNOWN_IDS =
            Set.of("google", "github", "apple", "discord", "gitlab", "line", "slack");

    private final Environment environment;

    public SocialProviders(Environment environment) {
        this.environment = environment;
    }

    public boolean anyEnabled() {
        return !listEnabled().isEmpty();
    }

    public boolean isEnabled(String providerId) {
        return listEnabled().stream().anyMatch(p -> p.id().equalsIgnoreCase(providerId));
    }

    public static boolean anySocialEnabled(Environment env) {
        for (ProviderDef d : CATALOG) {
            if (has(env, "spring.security.oauth2.client.registration." + d.id() + ".client-id")) {
                return true;
            }
        }
        return false;
    }

    public List<Provider> listEnabled() {
        List<Provider> list = new ArrayList<>();
        for (ProviderDef d : CATALOG) {
            if (has("spring.security.oauth2.client.registration." + d.id() + ".client-id")) {
                list.add(new Provider(
                        d.id(),
                        d.label(),
                        "/oauth2/authorization/" + d.id(),
                        d.protocol(),
                        d.scopes(),
                        d.envClientId()));
            }
        }
        return list;
    }

    public Map<String, Object> toPublicBody() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (Provider p : listEnabled()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id());
            m.put("label", p.label());
            m.put("authorizationUrl", p.authorizationPath());
            m.put("linkAuthorizationUrl", p.authorizationPath() + "?aaax_link=1");
            m.put("protocol", p.protocol());
            m.put("scopes", p.scopes());
            providers.add(m);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("providers", providers);
        body.put("enabled", !providers.isEmpty());
        body.put("supportedCatalog", CATALOG.stream().map(ProviderDef::id).toList());
        body.put("callbackPattern", "{issuer}/login/oauth2/code/{registrationId}");
        body.put(
                "notes",
                List.of(
                        "Empty client-id = provider off. Env: GOOGLE_*, GITHUB_*, APPLE_*, DISCORD_*, GITLAB_*, LINE_CHANNEL_*, SLACK_*.",
                        "Redirect URI per provider: {AAAX_ISSUER}/login/oauth2/code/{id}",
                        "Apple: client-secret is a short-lived JWT (ES256) you generate; set APPLE_CLIENT_SECRET.",
                        "Query: aaax_return=/user/ · aaax_link=1 attaches to current session."));
        return body;
    }

    private boolean has(String key) {
        return StringUtils.hasText(environment.getProperty(key));
    }

    private static boolean has(Environment env, String key) {
        return StringUtils.hasText(env.getProperty(key));
    }

    public record ProviderDef(String id, String label, String protocol, List<String> scopes, String envClientId) {}

    public record Provider(
            String id, String label, String authorizationPath, String protocol, List<String> scopes, String envClientId) {}

    public static String normalize(String provider) {
        return provider == null ? "" : provider.toLowerCase(Locale.ROOT).trim();
    }
}
