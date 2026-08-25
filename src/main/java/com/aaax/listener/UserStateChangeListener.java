package com.aaax.listener;


import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserStateMutatedEvent;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.config.security.RedisOAuth2AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Component;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_STATE_CHANGED;


@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-state-change-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserStateChangeListener extends BaseListener {

    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;
    private final DiscordApiClient discordApiClient;
    private final OAuth2AuthorizationService redisOauth2AuthorizationService;


    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserStateMutatedEvent event = JSONUtil.readValue(payload.value(), UserStateMutatedEvent.class);
        ((RedisOAuth2AuthorizationService) redisOauth2AuthorizationService).cleanUp(IdSplitter.split(event.getUserId()));
        discord(payload);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                .content("```".concat(payload.value()).concat("```"))
                .build();
        RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
    }

    @KafkaListener(
            topics = USER_STATE_CHANGED, groupId = USER_STATE_CHANGED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_STATE_CHANGED() {
        return new NewTopic(USER_STATE_CHANGED, 1, (short) 1);
    }
}
