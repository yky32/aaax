package com.aaax.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.constant.enu.Locale;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.constant.enu.NotificationAction;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.constant.enu.NotificationFrequency;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import com.aaax.core.kafka.event.UserCreatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.usecase.SystemConfigurationUseCase;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.aaax.core.kafka.enu.KafkaTopic.NOTIFICATION_MASS;
import static com.aaax.core.kafka.enu.KafkaTopic.USER_CREATED;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-created-notification-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserCreatedNotificationListener extends BaseListener {

    private final KafkaUtil kafkaUtil;
    private final DiscordApiClient discordApiClient;
    private final SystemConfigurationUseCase systemConfigurationUseCase;
    private final UserRepository userRepository;
    @Value("${config.system-invoker}")
    protected String systemInvoker;
    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserCreatedEvent event = JSONUtil.readValue(payload.value(), UserCreatedEvent.class);
        Optional<User> isExistedUser = userRepository.findById(event.getUserId());
        if (isExistedUser.isPresent()) {
            return; // NO repeat for notification
        }
        // 1. EMAIL Notifications [logic between PMS or PG]

        GetSystemConfigurationRequestDto config = systemConfigurationUseCase.query("NOTIFICATION", "USER_CREATED_EVENT_TEMPLATE_ID");
        log.info("==== uaa user created config: {}", config);
        Map configMap = JSONUtil.convertFromObject(config.getValue(), Map.class);
        Map<String, Object> parameterMap = Map.of(
                "templateId", configMap.getOrDefault("templateId", "N/A").toString(),
                "username", event.getUsername(),
                "loginUrl", configMap.getOrDefault("loginUrl", "N/A").toString()
        );
        CreateMassNotificationRequestDto notificationRequestDto = CreateMassNotificationRequestDto.builder()
                .notificationTemplateName(configMap.getOrDefault("notificationTemplateName", "N/A").toString())
                .channels(List.of(NotificationChannel.EMAIL))
                .parameterMap(parameterMap)
                .locale(List.of(Locale.EN))
                .systemInvoker(systemInvoker)
                .notificationAction(NotificationAction.REALTIME)
                .notificationFrequency(NotificationFrequency.ONE_OFF)
                .recipients(List.of(BaseNotificationEvent.builder()
                        .to(event.getUsername())
                        .build()))
                .build();
        kafkaUtil.send(NOTIFICATION_MASS, notificationRequestDto);

        // 2. Discord Notifications
        discord(payload);
    }

    private void discord(ConsumerRecord<String, String> payload) {
        DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                .content("```".concat(payload.value()).concat("```"))
                .build();
        RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
    }

    @KafkaListener(
            topics = USER_CREATED, groupId = USER_CREATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_CREATED() {
        return new NewTopic(USER_CREATED, 1, (short) 1);
    }
}
