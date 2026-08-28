package com.aaax.server.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.support.LoginSmokeAccounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional first-clone seed ({@code AAAX_LOCAL_SEED=true}).
 * Not for production.
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "aaax", name = "local-seed", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class LocalDevSeed implements CommandLineRunner {

    static final String LOCAL_CLIENT_ROW_ID = "760cc5ca-b513-4ce9-9e89-185ccbe1a403";

    static final String OAUTH2_REGISTERED_CLIENT_DDL = """
            CREATE TABLE IF NOT EXISTS oauth2_registered_client (
                id varchar(100) NOT NULL,
                client_id varchar(100) NOT NULL,
                client_id_issued_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
                client_secret varchar(200) DEFAULT NULL,
                client_secret_expires_at timestamp DEFAULT NULL,
                client_name varchar(200) NOT NULL,
                client_authentication_methods varchar(1000) NOT NULL,
                authorization_grant_types varchar(1000) NOT NULL,
                redirect_uris varchar(1000) DEFAULT NULL,
                post_logout_redirect_uris varchar(1000) DEFAULT NULL,
                scopes varchar(1000) NOT NULL,
                client_settings varchar(2000) NOT NULL,
                token_settings varchar(2000) NOT NULL,
                PRIMARY KEY (id)
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.warn("AAAX local seed is ON — client '{}' / user '{}' (not for production)",
                LoginSmokeAccounts.OAUTH_CLIENT_ID,
                LoginSmokeAccounts.PRIMARY.canonicalEmail());
        jdbcTemplate.execute(OAUTH2_REGISTERED_CLIENT_DDL);
        seedClient();
        seedPrimaryUser();
    }

    private void seedClient() {
        if (registeredClientRepository.findByClientId(LoginSmokeAccounts.OAUTH_CLIENT_ID) != null) {
            log.info("local seed: OAuth client {} already present", LoginSmokeAccounts.OAUTH_CLIENT_ID);
            return;
        }
        RegisteredClient client = RegisteredClient
                .withId(LOCAL_CLIENT_ROW_ID)
                .clientId(LoginSmokeAccounts.OAUTH_CLIENT_ID)
                .clientName("AAAX local")
                .clientSecret(passwordEncoder.encode(LoginSmokeAccounts.OAUTH_CLIENT_SECRET))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey()))
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(new AuthorizationGrantType("refresh-token"))
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
                .redirectUri("http://127.0.0.1:8081/authorized")
                .scope(OidcScopes.OPENID)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        registeredClientRepository.save(client);
        log.info("local seed: inserted OAuth client {}", LoginSmokeAccounts.OAUTH_CLIENT_ID);
    }

    private void seedPrimaryUser() {
        String username = LoginSmokeAccounts.PRIMARY.canonicalEmail();
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            log.info("local seed: user {} already present", username);
            return;
        }
        User user = User.builder()
                .username(username)
                .status(UserStatus.ACTIVE)
                .sourceSystemTags(List.of("AAAX"))
                .build();
        Authentication authentication = Authentication.builder()
                .user(user)
                .identifier(username)
                .loginType(LoginType.EMAIL)
                .credentials(passwordEncoder.encode(LoginSmokeAccounts.PRIMARY.password()))
                .attempts(0)
                .build();
        List<Authentication> authentications = new ArrayList<>();
        authentications.add(authentication);
        user.setAuthentications(authentications);
        userRepository.saveAndFlush(user);
        log.info("local seed: inserted user {}", username);
    }
}
