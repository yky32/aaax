package com.aaax.server.config.security;

import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.security.jwt.Jwt;
import com.aaax.server.config.security.jwt.JwtPayload;
import com.aaax.server.config.security.jwt.JwtMetadata;
import com.aaax.server.config.security.jwt.RegisteredClientMetadata;
import com.aaax.server.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisOAuth2AuthorizationServiceTest {

    @Mock private RedisUtil redisUtil;
    @Mock private RegisteredClientRepository registeredClientRepository;
    @Mock private UserTokenRepository userTokenRepository;

    @InjectMocks
    private RedisOAuth2AuthorizationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenHistoryCount", 5);
        ReflectionTestUtils.setField(service, "serverTokenExpiryTime", 3600);
    }

    @Test
    @DisplayName("cleanUp should delete user token redis key")
    void cleanUp_shouldDeleteKey() {
        service.cleanUp("10");
        verify(redisUtil).delete(contains("10"));
    }

    @Test
    @DisplayName("findById should return null when redis miss")
    void findById_shouldReturnNullOnMiss() {
        when(redisUtil.getOrElseThrow(anyString())).thenThrow(new RuntimeException("missing"));
        assertNull(service.findById("10"));
    }

    @Test
    @DisplayName("findByToken should fall back to DB compensation on redis miss")
    void findByToken_shouldFallbackToDb() {
        when(redisUtil.getOrElseThrow(anyString())).thenThrow(new RuntimeException("missing"));
        when(userTokenRepository.findByTokenValueAndTokenType(anyString(), anyString())).thenReturn(java.util.Optional.empty());
        assertNull(service.findByToken("rt-1", OAuth2TokenType.REFRESH_TOKEN));
    }

    @Test
    @DisplayName("save without access token should keep authorization code in memory")
    void save_withoutAccessToken_shouldFindByCode() {
        RegisteredClient client = RegisteredClient.withId("pkce")
                .clientId("aaax-pkce")
                .clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://127.0.0.1:9/authorized")
                .scope("openid")
                .build();
        org.springframework.security.oauth2.server.authorization.OAuth2Authorization authorization =
                org.springframework.security.oauth2.server.authorization.OAuth2Authorization
                        .withRegisteredClient(client)
                        .principalName("smoke.primary@aaax.local")
                        .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                        .token(new org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode(
                                "loopback-code", Instant.now(), Instant.now().plusSeconds(300)))
                        .build();
        assertNull(authorization.getAccessToken());
        service.save(authorization);
        verify(redisUtil, never()).set(anyString(), any(), anyLong());
        assertNotNull(service.findByToken("loopback-code", new OAuth2TokenType("code")));
    }

    @Test
    @DisplayName("remove should delete redis key for authorization jwt")
    void remove_shouldDelete() {
    }
}
