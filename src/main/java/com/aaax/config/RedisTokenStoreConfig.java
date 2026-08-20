package com.aaax.config;

import com.aaax.auth.MagicLinkTokenStore;
import com.aaax.auth.RedisMagicLinkTokenStore;
import com.aaax.otp.OtpCodeStore;
import com.aaax.otp.RedisOtpCodeStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Optional Redis backing for OTP + magic-link tokens (multi-node).
 * Enable: {@code aaax.otp.store=redis} + Redis host/port.
 * Default store is in-memory (no Redis required).
 */
@Configuration
@ConditionalOnProperty(name = "aaax.otp.store", havingValue = "redis")
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
    OtpCodeStore otpCodeStore(StringRedisTemplate aaaxStringRedisTemplate) {
        return new RedisOtpCodeStore(aaaxStringRedisTemplate);
    }

    @Bean
    MagicLinkTokenStore magicLinkTokenStore(StringRedisTemplate aaaxStringRedisTemplate) {
        return new RedisMagicLinkTokenStore(aaaxStringRedisTemplate);
    }
}
