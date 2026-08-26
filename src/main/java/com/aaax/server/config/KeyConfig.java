package com.aaax.server.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class KeyConfig {

    public static final String KEY_ALIAS = "myKeys";
    public static final String KEY_STORE_FILE = "keys/encryption-key.jks";
    public static final String KEY_STORE_PASSWORD = "Pass!23456";

    @Value("${aaax.encryption.keystore:}")
    private String encryptionKeystorePath;
    @Value("${aaax.encryption.keystore-password:}")
    private String encryptionKeystorePassword;
    @Value("${aaax.encryption.keystore-alias:}")
    private String encryptionKeystoreAlias;

    @Bean
    public KeyPair keyPair() {
        try {
            KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(
                    encryptionKeystoreResource(),
                    resolve(encryptionKeystorePassword, KEY_STORE_PASSWORD).toCharArray());
            return ksFactory.getKeyPair(resolve(encryptionKeystoreAlias, KEY_ALIAS));
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

    private Resource encryptionKeystoreResource() {
        if (encryptionKeystorePath != null && !encryptionKeystorePath.isBlank()) {
            return new FileSystemResource(encryptionKeystorePath);
        }
        return new ClassPathResource(KEY_STORE_FILE);
    }

    private static String resolve(String override, String demoDefault) {
        return (override == null || override.isBlank()) ? demoDefault : override;
    }
}
