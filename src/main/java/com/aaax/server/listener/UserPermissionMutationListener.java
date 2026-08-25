package com.aaax.server.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RedisUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.server.usecase.AccessControlUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_USER_PERMISSION_MUTATED;


@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-permission-mutation-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserPermissionMutationListener extends BaseListener {

    private final DiscordApiClient discordApiClient;
    private final AccessControlUseCase accessControlUseCase;
    private final RedisUtil redisUtil;
    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserPermissionMutatedEvent event = JSONUtil.readValue(payload.value(), UserPermissionMutatedEvent.class);
        // ====== TAKE ACTION()
        accessControlUseCase.assignPermissionToUser(event);
        // ====== TAKE ACTION()
        discord(payload);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                .content("```".concat(payload.value()).concat("```"))
                .build();
        RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
    }

    @KafkaListener(
            topics = USER_USER_PERMISSION_MUTATED, groupId = USER_USER_PERMISSION_MUTATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_USER_PERMISSION_MUTATED() {
        return new NewTopic(USER_USER_PERMISSION_MUTATED, 2, (short) 1);
    }
}
