package com.aaax.server.endpoint.uaa_config;

import com.nimbusds.jose.jwk.RSAKey;
import com.aaax.server.utils.CryptographyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class KeysEndpointTest {

    private KeysEndpoint endpoint;
    private RSAKey rsaKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID("test")
                .build();
        endpoint = new KeysEndpoint(rsaKey);
    }

    @Test
    @DisplayName("getPublicKey should return base64 public key")
    void getPublicKey_shouldReturn() {
        assertTrue(endpoint.getPublicKey().getData().containsKey("publicKey"));
    }

    @Test
    @DisplayName("getPrivateKey should return base64 private key")
    void getPrivateKey_shouldReturn() {
        assertTrue(endpoint.getPrivateKey().getData().containsKey("privateKey"));
    }

    @Test
    @DisplayName("decrypt should delegate to CryptographyUtil")
    void decrypt_shouldDelegate() {
        try (MockedStatic<CryptographyUtil> crypto = mockStatic(CryptographyUtil.class)) {
            crypto.when(() -> CryptographyUtil.decrypt(anyString(), any())).thenReturn("plain");
            assertEquals("plain", endpoint.decrypt(Map.of("q", "cipher")).getData().get("decryptedData"));
        }
    }
}
