package com.aaax.usecase.intf;

import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.config.redis.RedisKey;

public interface OtpHandler<R, D> {

    void mustValidations(RedisKey usecase, String to);
    R validationRecordInRedis(D dto);
    R convertRedisRecordTo(String redisKey);
    void setRedis(String redisKey, R dto, int ttl);
    void successAndCleanupRedis(String redisKey);
    void triggerNotification(BaseNotificationEvent event);
    void triggerNotification(BaseNotificationEvent event, String templateName);
}
