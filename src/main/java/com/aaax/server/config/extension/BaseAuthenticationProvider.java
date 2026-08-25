package com.aaax.server.config.extension;


import com.aaax.core.kafka.event.PostLoginSucceedEvent;
import com.aaax.core.utils.KafkaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_POST_LOGIN_SUCCEED;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseAuthenticationProvider {
    @Autowired
    protected KafkaUtil kafkaUtil;

    protected Map removeSensitiveInformation(Map map) {
        map.remove("credentials");
        map.remove("additionalParameters");
        return map;
    }

    protected void post_login_event(
            Long userId,
            String grantTypeEvent,
            Instant startTrafficDt,
            Map map,
            Object token
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("user", map);
        data.put("token", token);
        PostLoginSucceedEvent event = PostLoginSucceedEvent.builder()
                .domain("user-authentication")
                .event(grantTypeEvent)
                .userId(String.valueOf(userId))
                .startTrafficDt(startTrafficDt)
                .endTrafficDt(Instant.now())
                .requestBody(this.removeSensitiveInformation(map))
                .responseBody(data)
                .build();
        this.kafkaUtil.send(USER_POST_LOGIN_SUCCEED, event);
    }
}
