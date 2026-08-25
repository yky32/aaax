package com.aaax.config.extension.custom_password;

import com.aaax.core.utils.KafkaUtil;
import com.aaax.config.extension.GrantTypeExtension;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.user.User;
import com.aaax.service.AuthenticationService;
import com.aaax.usecase.GetMyLoginProfileUseCase;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPasswordAuthenticationProviderTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthenticationService authenticationService;
    @Mock private GetMyLoginProfileUseCase getMyLoginProfileUseCase;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private CustomPasswordAuthenticationProvider provider;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        provider = new CustomPasswordAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "authenticationService", authenticationService);
        ReflectionTestUtils.setField(provider, "getMyLoginProfileUseCase", getMyLoginProfileUseCase);
        ReflectionTestUtils.setField(provider, "kafkaUtil", kafkaUtil);

        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .build();
    }

    private CustomPasswordAuthenticationToken token() {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        return new CustomPasswordAuthenticationToken("user@test.com", "pass", clientPrincipal, null);
    }

    @Test
    @DisplayName("authenticate should throw when client not authenticated")
    void authenticate_shouldThrowWhenClientUnauthenticated() {
        when(clientPrincipal.isAuthenticated()).thenReturn(false);
        CustomPasswordAuthenticationToken auth =
                new CustomPasswordAuthenticationToken("user@test.com", "pass", clientPrincipal, null);
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
    }

    @Test
    @DisplayName("authenticate should throw when password check fails")
    void authenticate_shouldThrowWhenPasswordFails() {
        CustomPasswordAuthenticationToken auth = token();
        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(1L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);
        when(authenticationService.check(userAuth, "pass")).thenReturn(false);

        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
        verify(authenticationService).post_check(userAuth, false);
    }

    @Test
    @DisplayName("authenticate should quick-return existing redis authorization")
    void authenticate_shouldQuickReturnFromRedis() {
        CustomPasswordAuthenticationToken auth = token();
        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(9L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);
        when(authenticationService.check(userAuth, "pass")).thenReturn(true);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now().plusSeconds(60));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("9")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT.getKey()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        when(authorizationService.findById("9")).thenReturn(authorization);

        var result = provider.authenticate(auth);

        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, result);
        verify(authenticationService).post_check(userAuth, true);
        verify(kafkaUtil).send(anyString(), any());
        verify(tokenGenerator, never()).generate(any());
    }

    @Test
    @DisplayName("supports should accept custom password token")
    void supports_shouldAcceptToken() {
        assertTrue(provider.supports(CustomPasswordAuthenticationToken.class));
        assertFalse(provider.supports(String.class));
    }
}
