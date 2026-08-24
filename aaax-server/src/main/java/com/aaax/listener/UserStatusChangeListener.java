package com.aaax.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserStateMutatedEvent;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.config.security.RedisOAuth2AuthorizationService;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.service.UaaService;
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

import static com.aaax.core.kafka.enu.KafkaTopic.USER_USER_STATUS_MUTATED;


@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-status-change-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserStatusChangeListener extends BaseListener {

    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;
    private final DiscordApiClient discordApiClient;
    private final OAuth2AuthorizationService redisOauth2AuthorizationService;
    private final UaaService uaaService;
    private final UserRepository userRepository;


    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserStateMutatedEvent event = JSONUtil.readValue(payload.value(), UserStateMutatedEvent.class);
        ((RedisOAuth2AuthorizationService) redisOauth2AuthorizationService).cleanUp(IdSplitter.split(event.getUserId()));
        log.info(
                """
                === UserStatusChangeListener \n
                === event : {}
                """,
                event
        );
        // === TAKE action
        User user = uaaService.getById(event.getUserId());
        if (!event.getToBeStatus().equals(user.getStatus())) {
            user.setStatus(event.getToBeStatus());
            userRepository.save(user);
        }
        discord(payload);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                .content("```".concat(payload.value()).concat("```"))
                .build();
        RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
    }

    @KafkaListener(
            topics = USER_USER_STATUS_MUTATED, groupId = USER_USER_STATUS_MUTATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_USER_STATUS_MUTATED() {
        return new NewTopic(USER_USER_STATUS_MUTATED, 1, (short) 1);
    }
}
