package com.aaax.server.listener.worker;

import com.aaax.core.aop.log.LogScope;
import com.aaax.core.constant.enu.LogType;
import com.aaax.core.kafka.BaseListener;
import com.aaax.core.kafka.event.PostLoginSucceedEvent;
import com.aaax.core.utils.IdSplitter;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.config.security.jwt.Jwt;
import com.aaax.server.config.security.jwt.RegisteredClientMetadata;
import com.aaax.server.entity.enu.UserTokenType;
import com.aaax.server.entity.po.log.AuthenticationLog;
import com.aaax.server.entity.po.user_token.UserToken;
import com.aaax.server.repository.AuthenticationLogRepository;
import com.aaax.server.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.aaax.core.kafka.enu.KafkaTopic.USER_POST_LOGIN_SUCCEED;

@Component
@ConditionalOnProperty(prefix = "aaax.kafka", name = "enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class PostLoginSucceedWorker extends BaseListener {

    @Value("${spring.application.name}")
    private String serviceName;
    private final UserTokenRepository userTokenRepository;
    private final AuthenticationLogRepository authenticationLogRepository;

    // ______ this is the execute method you need to better implemented
    @Override
    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {

        PostLoginSucceedEvent event = JSONUtil.readValue(payload.value(), PostLoginSucceedEvent.class);
        Long trafficTimeInMilliseconds = Duration.between(event.getStartTrafficDt(), event.getEndTrafficDt()).toMillis();
        String traceId = event.getRequestId();

        AuthenticationLog _log = AuthenticationLog.builder().build();
        _log.setType(LogType.ACTIVITY);
        _log.setLogScope(LogScope.ENDPOINT);
        _log.setScope("INT");
        _log.setSystem(serviceName);
        _log.setDomain(event.getDomain());
        _log.setEvent(event.getEvent());
        _log.setRequestBody(event.getRequestBody());
        _log.setResponseBody(event.getResponseBody());
        _log.setContent(event);
        _log.setActionBy(event.getUserId());
        _log.setTrafficTimeInMilliseconds(trafficTimeInMilliseconds);
        _log.setTraceId(traceId);
        _log.setCorrelationId(event.getUserId());
        _log = authenticationLogRepository.saveAndFlush(_log);


        // ==== TOKEN storage
        String correlationId = "AUTH_LOG_".concat(_log.getCorrelationId());
        Map responseBody = JSONUtil.convertFromObject(event.getResponseBody(), Map.class);
        Map tokenUser = JSONUtil.convertFromObject(responseBody.getOrDefault("user", new HashMap<>()), Map.class);
        Map tokenData = JSONUtil.convertFromObject(responseBody.getOrDefault("token", new HashMap<>()), Map.class);
        Map grantType = (Map) tokenUser.getOrDefault("grantType", new HashMap<>());
        Map registeredClient = (Map) tokenData.getOrDefault("registeredClient", new HashMap<>());


        List<UserToken> dbUserTokens = new ArrayList<>();
        Jwt accessTokenForRT_fetching = this.commonJwtFields(tokenUser, grantType, registeredClient);


        tokenData.computeIfPresent("accessToken", (key, value) -> {
            Map<String, Object> _map = (Map<String, Object>) value;
            Instant issuedAt = Instant.ofEpochSecond(((Double) _map.get("issuedAt")).longValue());
            Instant expiresAt = Instant.ofEpochSecond(((Double) _map.get("expiresAt")).longValue());
            String tokenValue = (String) _map.get("tokenValue");

            Jwt jwt = this.commonJwtFields(tokenUser, grantType, registeredClient);
            jwt.setAccessToken(tokenValue);
            jwt.setAccessTokenIssuedAt(issuedAt);
            jwt.setAccessTokenExpiresAt(expiresAt);
            jwt.setExpiresIn(Objects.requireNonNull(expiresAt).getEpochSecond());

            accessTokenForRT_fetching.setAccessToken(tokenValue);
            accessTokenForRT_fetching.setAccessTokenIssuedAt(issuedAt);
            accessTokenForRT_fetching.setAccessTokenExpiresAt(expiresAt);
            accessTokenForRT_fetching.setExpiresIn(Objects.requireNonNull(expiresAt).getEpochSecond());

            UserToken userToken = UserToken.builder()
                    .userId(Long.valueOf(IdSplitter.split(event.getUserId())))
                    .issuedAt(issuedAt)
                    .expireAt(expiresAt)
                    .type(UserTokenType.ACCESS_TOKEN)
                    .traceId(traceId)
                    .correlationId(correlationId)
                    .value(jwt)
                    .build();
            dbUserTokens.add(userToken);
            return value; // Return the value unchanged
        });

        tokenData.computeIfPresent("refreshToken", (key, value) -> {
            Map<String, Object> _map = (Map<String, Object>) value;
            Instant issuedAt = Instant.ofEpochSecond(((Double) _map.get("issuedAt")).longValue());
            Instant expiresAt = Instant.ofEpochSecond(((Double) _map.get("expiresAt")).longValue());
            String tokenValue = (String) _map.get("tokenValue");

            Jwt jwt = this.commonJwtFields(tokenUser, grantType, registeredClient);
            jwt.setPrincipalName(accessTokenForRT_fetching.getPrincipalName());
            jwt.setAccessToken(accessTokenForRT_fetching.getAccessToken());
            jwt.setAccessTokenIssuedAt(accessTokenForRT_fetching.getAccessTokenIssuedAt());
            jwt.setAccessTokenExpiresAt(accessTokenForRT_fetching.getAccessTokenExpiresAt());
            jwt.setExpiresIn(accessTokenForRT_fetching.getExpiresIn());
            jwt.setRefreshToken(tokenValue);
            jwt.setRefreshTokenIssuedAt(issuedAt);
            jwt.setRefreshTokenExpiresAt(expiresAt);

            UserToken userToken = UserToken.builder()
                    .userId(Long.valueOf(IdSplitter.split(event.getUserId())))
                    .issuedAt(issuedAt)
                    .expireAt(expiresAt)
                    .type(UserTokenType.REFRESH_TOKEN)
                    .traceId(traceId)
                    .correlationId(correlationId)
                    .value(jwt)
                    .build();
            dbUserTokens.add(userToken);
            return value; // Return the value unchanged
        });

        if (!dbUserTokens.isEmpty()) {
            userTokenRepository.saveAll(dbUserTokens);
        }
    }

    @NotNull
    private Jwt commonJwtFields(Map tokenUser, Map grantType, Map registeredClient) {
        Jwt jwt = Jwt.builder().build();
        jwt.setPrincipalName((String) tokenUser.get("username"));
        jwt.setAuthorizationGrantType((String) grantType.get("value"));
        jwt.setRegisteredClientMetadata(RegisteredClientMetadata.builder().id((String) registeredClient.get("id")).build());
        jwt.setScopes(Set.of());
        return jwt;
    }

    @KafkaListener(
            topics = USER_POST_LOGIN_SUCCEED, groupId = USER_POST_LOGIN_SUCCEED + "-group",
            containerFactory = "consumerFactory"
    )
    private void action(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // ___ *******
        // ____ plz keep this method --> in-order-to align all method for kafka behavior
        super.listener(payload, ack);
    }

    @Bean
    public NewTopic USER_POST_LOGIN_SUCCEED() {
        return new NewTopic(USER_POST_LOGIN_SUCCEED, 1, (short) 1);
    }
}
