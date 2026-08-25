package com.aaax.server.config.security;

import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.aaax.server.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServerConfigBeansTest {

    @Mock private HttpServletRequest request;
    @Mock private GlobalExceptionHandler globalExceptionHandler;

    private AuthenticationServerConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthenticationServerConfig(request, globalExceptionHandler);
        ReflectionTestUtils.setField(config, "issuerUrl", "https://uaa.test");
        ReflectionTestUtils.setField(config, "jwkSetUri", "https://uaa.test/jwks");
        ReflectionTestUtils.setField(config, "serverTokenExpiryTime", 3600);
    }

    @Test
    @DisplayName("passwordEncoder should return BCrypt encoder")
    void passwordEncoder_shouldReturnBcrypt() {
        PasswordEncoder encoder = config.passwordEncoder();
        assertTrue(encoder.matches("secret", encoder.encode("secret")));
    }

    @Test
    @DisplayName("authorizationServerSettings should use issuer")
    void authorizationServerSettings_shouldUseIssuer() {
        AuthorizationServerSettings settings = config.authorizationServerSettings();
        assertEquals("https://uaa.test", settings.getIssuer());
    }

    @Test
    @DisplayName("keyPair should load from classpath keystore")
    void keyPair_shouldLoadFromClasspath() {
        KeyPair keyPair = config.keyPair();
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic());
    }

    @Test
    @DisplayName("jwkSource should expose RSA key")
    void jwkSource_shouldExposeRsaKey() throws Exception {
        List keys = config.jwkSource().get(new JWKSelector(new JWKMatcher.Builder().build()), null);
        assertFalse(keys.isEmpty());
    }

    @Test
    @DisplayName("userDetailsService should be JwtUserDetailsService")
    void userDetailsService_shouldBeJwtType() {
        assertInstanceOf(JwtUserDetailsService.class, config.userDetailsService());
    }

    @Test
    @DisplayName("authorizationService should return Redis implementation")
    void authorizationService_shouldReturnRedis() {
        assertInstanceOf(RedisOAuth2AuthorizationService.class, config.authorizationService());
    }

    @Test
    @DisplayName("tokenGenerator should be non-null")
    void tokenGenerator_shouldBeNonNull() {
        assertNotNull(config.tokenGenerator());
    }
}
