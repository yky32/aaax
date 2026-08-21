package com.aaax.config;

import com.aaax.auth.MagicLinkTokenStore;
import com.aaax.auth.QrLoginSessionStore;
import com.aaax.auth.RedisMagicLinkTokenStore;
import com.aaax.auth.RedisQrLoginSessionStore;
import com.aaax.otp.OtpCodeStore;
import com.aaax.otp.RedisOtpCodeStore;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Optional Redis for OTP / magic-link / QR sessions (multi-node).
 * Enable any of: {@code aaax.otp.store=redis}, {@code aaax.qr.store=redis}.
 */
@Configuration
@ConditionalOnExpression(
        "'${aaax.otp.store:memory}'.equalsIgnoreCase('redis') || '${aaax.qr.store:memory}'.equalsIgnoreCase('redis')")
public class RedisTokenStoreConfig {

    @Bean
    LettuceConnectionFactory aaaxRedisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database) {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration(host, port);
        conf.setDatabase(database);
        if (password != null && !password.isBlank()) {
            conf.setPassword(password);
        }
        return new LettuceConnectionFactory(conf);
    }

    @Bean
    StringRedisTemplate aaaxStringRedisTemplate(LettuceConnectionFactory aaaxRedisConnectionFactory) {
        return new StringRedisTemplate(aaaxRedisConnectionFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "aaax.otp.store", havingValue = "redis")
    OtpCodeStore otpCodeStore(StringRedisTemplate aaaxStringRedisTemplate) {
        return new RedisOtpCodeStore(aaaxStringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "aaax.otp.store", havingValue = "redis")
    MagicLinkTokenStore magicLinkTokenStore(StringRedisTemplate aaaxStringRedisTemplate) {
        return new RedisMagicLinkTokenStore(aaaxStringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "aaax.qr.store", havingValue = "redis")
    QrLoginSessionStore qrLoginSessionStore(
            StringRedisTemplate aaaxStringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${aaax.qr.ttl-seconds:120}") int ttlSeconds) {
        return new RedisQrLoginSessionStore(aaaxStringRedisTemplate, objectMapper, ttlSeconds);
    }
}
