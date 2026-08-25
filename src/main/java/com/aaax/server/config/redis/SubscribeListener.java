package com.aaax.server.config.redis;

import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
public class SubscribeListener implements MessageListener {

    private Session session;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("-- SubscribeListener.onMessage : {}", message);
        String msg = new String(message.getBody());
        if (ObjectUtils.isNotEmpty(session) && session.isOpen()) {
            session.getAsyncRemote().sendText(msg);
        }
    }
}
