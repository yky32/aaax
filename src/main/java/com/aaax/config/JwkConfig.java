package com.aaax.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * File-backed RSA JWK so tokens remain valid across restarts.
 */
@Configuration
public class JwkConfig {

    private static final Logger log = LoggerFactory.getLogger(JwkConfig.class);

    @Bean
    JWKSource<SecurityContext> jwkSource(@Value("${aaax.jwk.path:./data/aaax-jwk.json}") String path) {
        RSAKey rsaKey = loadOrCreate(Path.of(path));
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private static RSAKey loadOrCreate(Path path) {
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path, StandardCharsets.UTF_8);
                JWKSet set = JWKSet.parse(json);
                RSAKey key = (RSAKey) set.getKeys().getFirst();
                log.info("Loaded JWK from {}", path.toAbsolutePath());
                return key;
            }
            RSAKey generated = generateRsa();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, new JWKSet(generated).toString(false), StandardCharsets.UTF_8);
            log.info("Generated new JWK at {}", path.toAbsolutePath());
            return generated;
        } catch (IOException | java.text.ParseException ex) {
            throw new IllegalStateException("Unable to load or create JWK at " + path, ex);
        }
    }

    private static RSAKey generateRsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
