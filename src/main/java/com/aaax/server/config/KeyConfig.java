package com.aaax.server.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;


import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.security.KeyPair;

@Configuration
public class KeyConfig {

    public static final String KEY_ALIAS = "myKeys";
    public static final String KEY_STORE_FILE = "keys/encryption-key.jks";
    public static final String KEY_STORE_PASSWORD = "Pass!23456";

    @Bean
    public KeyPair keyPair() {
        try {
            // Load the KeyStore
            ClassPathResource ksFile = new ClassPathResource(KEY_STORE_FILE);
            KeyStoreKeyFactory ksFactory = new KeyStoreKeyFactory(ksFile, KEY_STORE_PASSWORD.toCharArray());
            return ksFactory.getKeyPair(KEY_ALIAS);
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
                .keyID("altech-uaa")
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }
}