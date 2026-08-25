package com.aaax.listener.use_case;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.api.dto.DiscordWebhookMessage;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.UserRoutesCreatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.entity.po.user.Authentication;
import com.aaax.entity.po.UserRoute;
import com.aaax.repository.UserRouteRepository;
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
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_USER_ROUTES_CREATED;


@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "spring.kafka.consumers.user-with-user-route-creation-listener",
        name = "isEnabled", havingValue = "true"
)
public class UserWithUserRoutesCreationListener extends BaseListener {

    @Value("${ext.api.client.discord.webhookId}")
    private String webhookId;
    @Value("${ext.api.client.discord.webhookToken}")
    private String webhookToken;
    private final UserRouteRepository userRouteRepository;
    private final DiscordApiClient discordApiClient;
    private final UaaService uaaService;


    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        UserRoutesCreatedEvent event = JSONUtil.readValue(payload.value(), UserRoutesCreatedEvent.class);
        log.info("======= UserWithUserRoutesCreationListener => event {}", event);
        Authentication authentication = uaaService.getByUsername(event.getUsername());
        Long tenantRoleRouteId = Long.valueOf(event.getTenantRoleRouteId());
        Optional<UserRoute> isExistedUserRoute = userRouteRepository.findByTenantRoleRouteIdAndUserId(tenantRoleRouteId, authentication.getUser().getId());
        UserRoute userRoute = isExistedUserRoute.orElseGet(() -> UserRoute.builder()
                .userId(authentication.getUser().getId())
                .actualRoutes(event.getRoutes())
                .tenantRoleRouteId(tenantRoleRouteId)
                .build());
        userRoute = userRouteRepository.save(userRoute);
        this.elk(JSONUtil.writeValue(event));
        log.info("======= UserWithUserRoutesCreationListener => userRoute {}", userRoute);
    }

    @Override
    public void elk(String _message) {
        try {
            DiscordWebhookMessage message = DiscordWebhookMessage.builder()
                    .username("Last Step: ⬆️ UserWithUserRoutesCreationListener # ")
                    .content("```".concat(_message).concat("```"))
                    .build();
            RetrofitCallHandler._void_execute(discordApiClient.sendWebhookMessage(message, webhookId, webhookToken));
        } catch (Exception exception) {
            log.info("---- // ===== alert => {}", exception.getMessage());
        }
    }

    @KafkaListener(
            topics = USER_USER_ROUTES_CREATED, groupId = USER_USER_ROUTES_CREATED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_USER_ROUTES_CREATED() {
        return new NewTopic(USER_USER_ROUTES_CREATED, 2, (short) 1);
    }
}
