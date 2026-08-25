package com.aaax.server.endpoint.ws;

import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.utils.ApplicationContextUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RandomHashGenerator;
import com.aaax.core.utils.RedisUtil;
import com.aaax.server.config.redis.RedisKey;
import com.aaax.server.config.redis.SubscribeListener;
import com.aaax.server.config.websocket.WebSocketConfigurator;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@ServerEndpoint(value = "/ws/devices/{deviceId}", configurator = WebSocketConfigurator.class)
public class QrCodeLoginSocketServer {
    // =============== Configuration and Share Session in Redis.
    private RedisUtil redisUtil;
    private RedisMessageListenerContainer redisMessageListenerContainer;
    private SubscribeListener subscribeListener; // => plz create this class per request/ws.
    private StringRedisTemplate stringRedisTemplate;
    private KafkaUtil kafkaUtil;
    // =============== Configuration and Share Session in Redis.

    @OnOpen
    public void onOpen(@PathParam("deviceId") String deviceId, Session session) {
        initSpringBean();
        String sessionId = sessionId();
        createSession(deviceId, sessionId);
        setUpRedisListener(sessionId, session);
        addOnlineDevice(deviceId);
        log.info("-- QR grant device connected: {} -> {}, onlineDevicesCount: {}", deviceId, sessionId, getOnlineDevices());
    }

    @OnMessage
    public void onMessage(@PathParam("deviceId") String deviceId, Session session, String message) {
        log.info("-- Received message from device: {} => {}", deviceId, message);
    }

    @OnClose
    public void onClose(@PathParam("deviceId") String deviceId, Session session, CloseReason reason) {
        removeOnlineDevice(deviceId);
        this.redisMessageListenerContainer.removeMessageListener(subscribeListener);
        log.info("-- Device: {} disconnected, latest count:{}, reason => {}", deviceId, getOnlineDevices(), reason);
    }

    @OnError
    public void onError(@PathParam("deviceId") String deviceId, Session session, Throwable throwable) {
        log.info("-- Client: {} error, reason => {}", deviceId, throwable.getMessage());
    }

    // ==== util method
    public void addOnlineDevice(String deviceId) {
        this.redisUtil.hset(RedisKey.DEVICE_SESSIONS.getKey(), deviceId, UUID.randomUUID().toString());
    }

    public void removeOnlineDevice(String deviceId) {
        this.redisUtil.hashDelete(RedisKey.DEVICE_SESSIONS.getKey(), deviceId);
    }

    private int getOnlineDevices() {
        return redisUtil.hmget(RedisKey.DEVICE_SESSIONS.getKey()).size();
    }

    @SneakyThrows
    public void sendMessage(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    private String sessionId() {
        return RandomHashGenerator.generateRandomHash(30);
    }

    private void createSession(String deviceId, String sessionId) {
        String userSessionKeyInRedis = getSessionKeyInRedis(deviceId);
        kickOut(userSessionKeyInRedis);
        // －－ mark down the session per token request.
        log.info("-- 3. Create Session: {} -> {}", userSessionKeyInRedis, sessionId);
        redisUtil.set(userSessionKeyInRedis, sessionId, 2592000); // 30 days
    }

    private void kickOut(String sessionKeyInRedis) {
        log.info("-- 1. Check kick-out: {}", sessionKeyInRedis);
        String sessionId = (String) redisUtil.get(sessionKeyInRedis);
        if (sessionId == null) {
            log.info("-- 2. Nothing to kick-out: {}", sessionKeyInRedis);
            return;
        }
        log.info("-- 2. Forced kick-out: {} -> {}", sessionKeyInRedis, sessionId);
        stringRedisTemplate.convertAndSend(sessionId, KafkaTopic.USER_AUTH_FORCED_LOGOUT);
    }

    /**
     * IOC -> getting back Spring Bean from [spring-web-container]
     */
    private void initSpringBean() {
        this.redisMessageListenerContainer = ApplicationContextUtil.getBean(RedisMessageListenerContainer.class);
        this.redisUtil = ApplicationContextUtil.getBean(RedisUtil.class);
        this.stringRedisTemplate = ApplicationContextUtil.getBean(StringRedisTemplate.class);
        this.kafkaUtil = ApplicationContextUtil.getBean(KafkaUtil.class);
    }

    private void setUpRedisListener(String sessionId, Session session) {
        this.subscribeListener = new SubscribeListener(session);
        ChannelTopic authSessionTopic = new ChannelTopic(sessionId);
        this.redisMessageListenerContainer.addMessageListener(subscribeListener, authSessionTopic);
    }

    @NotNull
    private String getSessionKeyInRedis(String deviceId) {
        return RedisKey.DEVICE_SESSIONS.getKey().concat(deviceId);
    }
}
