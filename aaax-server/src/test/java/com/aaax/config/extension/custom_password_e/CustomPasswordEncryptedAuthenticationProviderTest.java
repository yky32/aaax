package com.aaax.config.extension.custom_password_e;

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
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomPasswordEncryptedAuthenticationProviderTest {

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private OAuth2TokenGenerator<?> tokenGenerator;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuthenticationService authenticationService;
    @Mock private GetMyLoginProfileUseCase getMyLoginProfileUseCase;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private OAuth2ClientAuthenticationToken clientPrincipal;

    private CustomPasswordEncryptedAuthenticationProvider provider;
    private RegisteredClient registeredClient;
    private final String encrypted = Base64.getEncoder().encodeToString("plain".getBytes());

    @BeforeEach
    void setUp() {
        provider = new CustomPasswordEncryptedAuthenticationProvider(authorizationService, tokenGenerator, authenticationManager);
        ReflectionTestUtils.setField(provider, "authenticationService", authenticationService);
        ReflectionTestUtils.setField(provider, "getMyLoginProfileUseCase", getMyLoginProfileUseCase);
        ReflectionTestUtils.setField(provider, "kafkaUtil", kafkaUtil);
        registeredClient = RegisteredClient.withId("rc-1")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey()))
                .build();
    }

    private CustomPasswordEncryptedAuthenticationToken token(String credentials) {
        when(clientPrincipal.isAuthenticated()).thenReturn(true);
        when(clientPrincipal.getRegisteredClient()).thenReturn(registeredClient);
        return new CustomPasswordEncryptedAuthenticationToken("user@test.com", credentials, clientPrincipal, null);
    }

    @Test
    @DisplayName("authenticate should reject non-base64 credentials")
    void authenticate_shouldRejectNonBase64() {
        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(token("not-base64!!!")));
    }

    @Test
    @DisplayName("authenticate should throw when password check fails")
    void authenticate_shouldThrowWhenPasswordFails() {
        CustomPasswordEncryptedAuthenticationToken auth = token(encrypted);
        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(1L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);
        when(authenticationService.decrypt(encrypted)).thenReturn("plain");
        when(authenticationService.check(userAuth, "plain")).thenReturn(false);

        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(auth));
        verify(authenticationService).post_check(userAuth, false);
    }

    @Test
    @DisplayName("authenticate should quick-return existing redis authorization")
    void authenticate_shouldQuickReturnFromRedis() {
        CustomPasswordEncryptedAuthenticationToken auth = token(encrypted);
        Authentication userAuth = Authentication.builder()
                .identifier("user@test.com")
                .user(User.builder().id(9L).build())
                .build();
        when(authenticationService.findValidRecordsByDynamicIdentifier("user@test.com")).thenReturn(userAuth);
        when(authenticationService.decrypt(encrypted)).thenReturn("plain");
        when(authenticationService.check(userAuth, "plain")).thenReturn(true);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access", Instant.now(), Instant.now().plusSeconds(60));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("9")
                .principalName("user@test.com")
                .authorizationGrantType(new AuthorizationGrantType(GrantTypeExtension.CUSTOM_PASSWORD_GRANT_ENCRYPTED.getKey()))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        when(authorizationService.findById("9")).thenReturn(authorization);

        assertInstanceOf(OAuth2AccessTokenAuthenticationToken.class, provider.authenticate(auth));
        verify(kafkaUtil).send(anyString(), any());
    }
}
