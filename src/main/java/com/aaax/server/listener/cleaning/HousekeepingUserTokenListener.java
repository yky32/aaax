package com.aaax.server.listener.cleaning;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.DiscordWebhookSupport;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.repository.UserTokenRepository;
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

import java.util.Map;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_HOUSEKEEPING_EXPIRED_USER_TOKENS;
import static com.aaax.core.kafka.enu.KafkaTopic.USER_HOUSEKEEPING_EXPIRED_USER_TOKENS;


@Component
@ConditionalOnProperty(prefix = "aaax.kafka", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class HousekeepingUserTokenListener extends BaseListener {

    private final DiscordApiClient discordApiClient;
    private final UserTokenRepository userTokenRepository;
    @Value("${ext.api.client.discord.webhookId:}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken:}")
    private String webhookToken;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        Map<String, Object> event = JSONUtil.readValue(payload.value(), Map.class);
        userTokenRepository.deleteById(IdSplitter.splitToLong(event.get("id")));
        log.info("-- User token deleted -- {}", event);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookSupport.sendSafe(discordApiClient, webhookId, webhookToken, "```".concat(payload.value()).concat("```"));
    }

    @KafkaListener(
            topics = USER_HOUSEKEEPING_EXPIRED_USER_TOKENS, groupId = USER_HOUSEKEEPING_EXPIRED_USER_TOKENS + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_HOUSEKEEPING_EXPIRED_USER_TOKENS() {
        return new NewTopic(USER_HOUSEKEEPING_EXPIRED_USER_TOKENS, 1, (short) 1);
    }
}
