package com.aaax.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserAliasGeneratedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.usecase.RegisterUserUseCase;
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

import static com.aaax.core.kafka.enu.KafkaTopic.USER_ALIAS_GENERATED;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-alias-generated-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserAliasGeneratedListener extends BaseListener {

    private final DiscordApiClient discordApiClient;
    private final RegisterUserUseCase registerUserUseCase;
    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserAliasGeneratedEvent event = JSONUtil.readValue(payload.value(), UserAliasGeneratedEvent.class);
        this.elk(JSONUtil.writeValue(event));
    }


    @Override
    public void elk(String _message) {
        try {
            DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                    .username("#UAA: user-alias generated # ")
                    .content("```".concat(_message).concat("```"))
                    .build();
            RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
        } catch (Exception exception) {
            log.info("---- // ===== alert => {}", exception.getMessage());
        }
    }


    @KafkaListener(
            topics = USER_ALIAS_GENERATED, groupId = USER_ALIAS_GENERATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_ALIAS_GENERATED() {
        return new NewTopic(USER_ALIAS_GENERATED, 1, (short) 1);
    }
}
