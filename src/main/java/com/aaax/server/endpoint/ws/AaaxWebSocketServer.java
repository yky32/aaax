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
@ServerEndpoint(value = "/ws/{jwt}", configurator = WebSocketConfigurator.class)
public class AaaxWebSocketServer {

    // =============== Configuration and Share Session in Redis.
    private RedisUtil redisUtil;
    private RedisMessageListenerContainer redisMessageListenerContainer;
    private SubscribeListener subscribeListener; // => plz create this class per request/ws.
    private StringRedisTemplate stringRedisTemplate;
    private KafkaUtil kafkaUtil;
    // =============== Configuration and Share Session in Redis.

    @OnOpen
    public void onOpen(@PathParam("jwt") String jwt, Session session) {
        initSpringBean();
        String userId = (String) session.getUserProperties().get("userId");
        String sessionId = sessionId();
        createSession(userId, sessionId);
        setUpRedisListener(sessionId, session);
        addOnlineUser(userId);
        log.info("-- Client connected: {} -> {}, onlineCount: {}", userId, sessionId, getOnlineUsers());
    }

    @OnMessage
    public void onMessage(@PathParam("jwt") String jwt, Session session, String message) {
        String userId = (String) session.getUserProperties().get("userId");
        log.info("-- Received message from client: {} => {}", userId, message);
    }

    @OnClose
    public void onClose(@PathParam("jwt") String jwt, Session session, CloseReason reason) {
        String userId = (String) session.getUserProperties().get("userId");
        removeOnlineUser(userId);
        this.redisMessageListenerContainer.removeMessageListener(subscribeListener);
        log.info("-- Client: {} disconnected, latest count:{}, reason => {}", userId, getOnlineUsers(), reason);
    }

    @OnError
    public void onError(@PathParam("jwt") String jwt, Session session, Throwable throwable) {
        String userId = (String) session.getUserProperties().get("userId");
        log.info("-- Client: {} error, reason => {}", userId, throwable.getMessage());
    }

    // ==== util method
    public void addOnlineUser(String userId) {
        this.redisUtil.hset(RedisKey.USER_AUTH_SESSIONS_COUNT.getKey(), userId, UUID.randomUUID().toString());
    }

    public void removeOnlineUser(String userId) {
        this.redisUtil.hashDelete(RedisKey.USER_AUTH_SESSIONS_COUNT.getKey(), userId);
    }

    private int getOnlineUsers() {
        return redisUtil.hmget(RedisKey.USER_AUTH_SESSIONS_COUNT.getKey()).size();
    }

    @SneakyThrows
    public void sendMessage(Session session, String message) {
        session.getAsyncRemote().sendText(message);
    }

    private String sessionId() {
        return RandomHashGenerator.generateRandomHash(30);
    }

    private void createSession(String userId, String sessionId) {
        String userSessionKeyInRedis = getSessionKeyInRedis(userId);
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
    private String getSessionKeyInRedis(String userId) {
        return RedisKey.USER_AUTH_SESSIONS.getKey().concat(userId);
    }
}
