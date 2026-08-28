package com.aaax.server.config.redis;

import com.aaax.core.utils.RedisUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;


@Configuration
@ConditionalOnClass(RedisOperations.class)
public class RedisConfig {

    /**
     * description:  Manual Register the RedisListener as Bean to IOC
     * @param: redisConnectionFactory
     * @return: RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        return container;
    }

    // ========================== redis config
    @Bean
    public RedisUtil redisUtil(RedisConnectionFactory redisConnectionFactory) {
        return new RedisUtil(redisTemplate(redisConnectionFactory));
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJacksonJsonRedisSerializer(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
