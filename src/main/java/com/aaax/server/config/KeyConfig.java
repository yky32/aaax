package com.aaax.server.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class KeyConfig {

    private static final Logger log = LoggerFactory.getLogger(KeyConfig.class);

    @Value("${aaax.encryption.keystore:}")
    private String encryptionKeystorePath;
    @Value("${aaax.encryption.keystore-password:}")
    private String encryptionKeystorePassword;
    @Value("${aaax.encryption.keystore-alias:}")
    private String encryptionKeystoreAlias;

    @Bean
    public KeyPair keyPair() {
        try {
            if (encryptionKeystorePath != null && !encryptionKeystorePath.isBlank()) {
                if (blank(encryptionKeystorePassword) || blank(encryptionKeystoreAlias)) {
                    throw new IllegalStateException(
                            "AAAX_ENCRYPTION_KEYSTORE is set; also set AAAX_ENCRYPTION_KEYSTORE_PASSWORD and AAAX_ENCRYPTION_KEYSTORE_ALIAS");
                }
                KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(
                        new FileSystemResource(encryptionKeystorePath),
                        encryptionKeystorePassword.toCharArray());
                return ksFactory.getKeyPair(encryptionKeystoreAlias);
            }
            log.warn("AAAX_ENCRYPTION_KEYSTORE unset — generating ephemeral RSA (not for production)");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load KeyPair", e);
        }
    }

    @Bean
    public RSAKey generateRsa(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("aaax")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
