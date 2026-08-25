package com.aaax.config.websocket;


import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.ApplicationContextUtil;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.config.redis.RedisKey;
import com.aaax.config.security.jwt.Jwt;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketConfigurator extends ServerEndpointConfig.Configurator {

    private RedisUtil redisUtil;
    private KafkaUtil kafkaUtil;

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        String wsHash = null;
        // skip wsHash checking when devices register in ws
        if (request.getRequestURI().getPath().contains("/ws/devices")) {
            return;
        }
        try {
            initSpringBean();
            wsHash = request.getRequestURI().getPath().substring(request.getRequestURI().getPath().lastIndexOf('/') + 1);
            String redisKey = RedisKey.USER_WS_HASH.getKey().concat(wsHash);
            Jwt jwt = JSONUtil.convertFromObject(redisUtil.getOrElseThrow(redisKey), Jwt.class);
            config.getUserProperties().put("userId", jwt.getPayload().getSub());
            log.info("-- WebSocketConfigurator, success in wsHash [{}].", wsHash);
        } catch (Exception exception) {
            log.info("-- WebSocketConfigurator, invalid wsHash for [ws] {}.", wsHash);
            throw new BizException(SystemResponse.SYS9999, "invalid wsHash for [ws].");
        }
    }

    private void initSpringBean() {
        this.redisUtil = ApplicationContextUtil.getBean(RedisUtil.class);
        this.kafkaUtil = ApplicationContextUtil.getBean(KafkaUtil.class);
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return super.checkOrigin(originHeaderValue);
    }
}
