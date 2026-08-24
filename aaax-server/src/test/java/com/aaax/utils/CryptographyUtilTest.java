package com.aaax.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptographyUtilTest {

    private static KeyPair keyPair;

    @BeforeAll
    static void setUpKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
    }

    @Test
    @DisplayName("decrypt should return plaintext for valid RSA ciphertext")
    void decrypt_shouldReturnPlaintext() throws Exception {
        String plaintext = "secret-password";
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        String encrypted = Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));

        String decrypted = CryptographyUtil.decrypt(encrypted, keyPair.getPrivate());

        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("decrypt should throw OAuth2AuthenticationException for invalid input")
    void decrypt_shouldThrowOnInvalidInput() {
        PrivateKey privateKey = keyPair.getPrivate();

        assertThrows(OAuth2AuthenticationException.class,
                () -> CryptographyUtil.decrypt("not-valid-base64!!!", privateKey));
    }
}
