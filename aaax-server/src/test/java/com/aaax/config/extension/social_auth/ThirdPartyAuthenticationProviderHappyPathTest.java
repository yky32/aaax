package com.aaax.config.extension.social_auth;

import com.aaax.core.utils.KafkaUtil;
import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.entity.po.user.User;
import com.aaax.usecase.SocialAuthenticationUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAuthenticationProviderHappyPathTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private SocialAuthenticationUseCase socialAuthenticationUseCase;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private ThirdPartyAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new ThirdPartyAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "socialAuthenticationUseCase", socialAuthenticationUseCase);
        ReflectionTestUtils.setField(provider, "kafkaUtil", kafkaUtil);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .scope("openid")
                .build();
        AuthorizationServerSettings settings = AuthorizationServerSettings.builder().issuer("https://uaa.test").build();
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override public String getIssuer() { return settings.getIssuer(); }
            @Override public AuthorizationServerSettings getAuthorizationServerSettings() { return settings; }
        });
    }

    @AfterEach
    void tearDown() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    @DisplayName("authenticate should mint tokens for Google when redis miss")
    void authenticate_shouldMintTokensForGoogle() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        when(clientPrincipal.getClientAuthenticationMethod()).thenReturn(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        when(socialAuthenticationUseCase.loginByGoogleOauth("id-token", "IOS"))
                .thenReturn(User.builder().id(11L).username("g@test.com").build());
        when(authorizationService.findById("11")).thenReturn(null);

        Instant now = Instant.now();
        Jwt accessJwt = Jwt.withTokenValue("access-jwt")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "11").build();
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", now, now.plusSeconds(3600));
        Jwt idJwt = Jwt.withTokenValue("id-jwt")
                .header("alg", "RS256").issuedAt(now).expiresAt(now.plusSeconds(300)).claim("sub", "11").build();
        when(tokenGenerator.generate(any(OAuth2TokenContext.class))).thenAnswer(inv -> {
            OAuth2TokenContext ctx = inv.getArgument(0);
            if (OAuth2TokenType.ACCESS_TOKEN.equals(ctx.getTokenType())) return accessJwt;
            if (GrantTypeExtension.CUSTOM_REFRESH_TOKEN.getKey().equals(ctx.getTokenType().getValue())) {
                return refreshToken;
            }
            if (OidcParameterNames.ID_TOKEN.equals(ctx.getTokenType().getValue())) return idJwt;
            return null;
        });

        ThirdPartyAuthenticationToken auth =
                new ThirdPartyAuthenticationToken("id-token", "GOOGLE", "IOS", clientPrincipal, null);
        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(authorizationService).save(any());
        verify(kafkaUtil).send(anyString(), any());
    }
}
