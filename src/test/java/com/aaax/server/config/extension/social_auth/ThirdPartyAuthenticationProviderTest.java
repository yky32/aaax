package com.aaax.server.config.extension.social_auth;

import com.aaax.core.exception.BizException;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.server.config.extension.GrantTypeExtension;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.usecase.SocialAuthenticationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThirdPartyAuthenticationProviderTest {

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
                .build();
    }

    private ThirdPartyAuthenticationToken token(String providerName) {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        return new ThirdPartyAuthenticationToken("id-token", providerName, "IOS", clientPrincipal, null);
    }

    @Test
    @DisplayName("supports should accept ThirdPartyAuthenticationToken")
    void supports_shouldAcceptToken() {
        assertTrue(provider.supports(ThirdPartyAuthenticationToken.class));
        assertFalse(provider.supports(String.class));
    }

    @Test
    @DisplayName("authenticate should reject invalid client")
    void authenticate_shouldRejectInvalidClient() {
        when(clientPrincipal.isAuthenticated()).thenReturn(false);
        ThirdPartyAuthenticationToken auth =
                new ThirdPartyAuthenticationToken("id-token", "GOOGLE", "IOS", clientPrincipal, null);
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
    }

    @Test
    @DisplayName("authenticate should reject unsupported provider")
    void authenticate_shouldRejectUnsupportedProvider() {
        ThirdPartyAuthenticationToken auth = token("FACEBOOK");
        assertThrows(BizException.class, () -> provider.authenticate(auth));
    }

    @Test
    @DisplayName("authenticate should quick-return existing redis authorization")
    void authenticate_shouldQuickReturnFromRedis() {
        ThirdPartyAuthenticationToken auth = token("GOOGLE");
        User user = User.builder().id(11L).username("g@test.com").build();
        when(socialAuthenticationUseCase.loginByGoogleOauth("id-token", "IOS")).thenReturn(user);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now().plusSeconds(60));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("11")
                .principalName("g@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        when(authorizationService.findById("11")).thenReturn(authorization);

        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(kafkaUtil).send(anyString(), any());
        verify(tokenGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("authenticate should quick-return for Apple when redis hit")
    void authenticate_shouldQuickReturnAppleFromRedis() {
        ThirdPartyAuthenticationToken auth = token("APPLE");
        User user = User.builder().id(22L).username("a@test.com").build();
        when(socialAuthenticationUseCase.loginByAppleOauth("id-token", "IOS")).thenReturn(user);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now().plusSeconds(60));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("22")
                .principalName("a@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.THIRD_PARTY_OAUTH_GRANT.getKey()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        when(authorizationService.findById("22")).thenReturn(authorization);

        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(socialAuthenticationUseCase).loginByAppleOauth("id-token", "IOS");
    }
}
