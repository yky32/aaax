package com.aaax.server.listener;


import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.DiscordWebhookSupport;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserDeletedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.server.usecase.UserManagementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_DELETED;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserDeletedNotificationListener extends BaseListener {

    private final KafkaUtil kafkaUtil;
    private final DiscordApiClient discordApiClient;
    private final UserManagementUseCase userManagementUseCase;
    @Value("${config.system-invoker}")
    protected String systemInvoker;
    @Value("${ext.api.client.discord.webhookId:}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken:}")
    private String webhookToken;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserDeletedEvent event = JSONUtil.readValue(payload.value(), UserDeletedEvent.class);
        // 1. EMAIL Notifications [logic between PMS or PG]
        userManagementUseCase.deleteByUserId(event.getUserId(), false);
        // 2. Discord Notifications
        this.elk(payload.value());
    }

    @Override
    public void elk(String message) {
        super.elk(message);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookSupport.sendSafe(discordApiClient, webhookId, webhookToken, "```".concat(payload.value()).concat("```"));
    }

    @KafkaListener(
            topics = USER_DELETED, groupId = USER_DELETED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_DELETED() {
        return new NewTopic(USER_DELETED, 1, (short) 1);
    }
}
