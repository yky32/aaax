package com.aaax.core.utils;

import com.aaax.core.constant.enu.Locale;
import com.aaax.core.constant.enu.NotificationAction;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.constant.enu.NotificationFrequency;
import com.aaax.core.exception.BizException;
import com.aaax.core.kafka.BaseNotificationEvent;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import com.aaax.core.kafka.enu.KafkaTopic;
import com.aaax.core.response.SystemResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
public class NotificationUtil {

    private NotificationUtil() {
    }

    public static void validateParams(CreateMassNotificationRequestDto event) {
        switch (event.getNotificationAction()) {
            case SCHEDULED -> {
                if (StringUtils.isBlank(event.getExecuteDt())) {
                    String message = String.format(
                            "[%s] type. [%s] is not given.",
                            event.getNotificationAction(),
                            "executeDt"
                    );
                    throw new BizException(SystemResponse.PAM0400, message);
                }
            }
            case REALTIME -> {
            }
        }

        switch (event.getNotificationFrequency()) {
            case RECURRING -> {
                if (StringUtils.isBlank(event.getExpireAt())) {
                    String message = String.format(
                            "[%s] type. [%s] is not given.",
                            event.getNotificationAction(),
                            "expireAt"
                    );
                    throw new BizException(SystemResponse.PAM0400, message);
                }

                if (StringUtils.isBlank(event.getCrontab())) {
                    String message = String.format(
                            "[%s] type. [%s] is not given.",
                            event.getCrontab(),
                            "crontab"
                    );
                    throw new BizException(SystemResponse.PAM0400, message);
                }
            }
        }
    }

    /**
     * Builds a realtime / one-off mass notification for a single recipient and channel.
     * {@code actionBy} is taken from {@code parameterMap.userId} when present (existing convention).
     */
    public static CreateMassNotificationRequestDto realtimeOneOff(
            String notificationTemplateName,
            NotificationChannel channel,
            String recipientTo,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        return realtimeOneOff(
                notificationTemplateName,
                channel,
                recipientTo == null ? List.of() : List.of(recipientTo),
                userLocal,
                parameterMap,
                systemInvoker
        );
    }

    /**
     * Builds a realtime / one-off mass notification for one or more recipients on a single channel.
     */
    public static CreateMassNotificationRequestDto realtimeOneOff(
            String notificationTemplateName,
            NotificationChannel channel,
            List<String> recipientTos,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        Locale resolvedLocal = userLocal != null ? userLocal : Locale.EN;
        List<String> recipients = recipientTos == null
                ? List.of()
                : recipientTos.stream()
                .filter(Objects::nonNull)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();

        return CreateMassNotificationRequestDto.builder()
                .notificationTemplateName(notificationTemplateName)
                .channels(List.of(channel))
                .parameterMap(parameterMap)
                .locale(NotificationLocaleUtil.localesWithFallback(resolvedLocal))
                .userLocal(resolvedLocal)
                .systemInvoker(systemInvoker)
                .notificationAction(NotificationAction.REALTIME)
                .notificationFrequency(NotificationFrequency.ONE_OFF)
                .recipients(recipients.stream()
                        .map(to -> BaseNotificationEvent.builder()
                                .to(to)
                                .userLocal(resolvedLocal)
                                .build())
                        .collect(Collectors.toList()))
                .actionBy(actionByFrom(parameterMap))
                .build();
    }

    public static CreateMassNotificationRequestDto realtimeAppPush(
            String notificationTemplateName,
            String fcmToken,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        return realtimeOneOff(
                notificationTemplateName,
                NotificationChannel.APP_PUSH,
                fcmToken,
                userLocal,
                parameterMap,
                systemInvoker
        );
    }

    public static CreateMassNotificationRequestDto realtimeAppPush(
            String notificationTemplateName,
            List<String> fcmTokens,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        return realtimeOneOff(
                notificationTemplateName,
                NotificationChannel.APP_PUSH,
                fcmTokens,
                userLocal,
                parameterMap,
                systemInvoker
        );
    }

    public static CreateMassNotificationRequestDto realtimeSms(
            String notificationTemplateName,
            String mobile,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        return realtimeOneOff(
                notificationTemplateName,
                NotificationChannel.SMS,
                mobile,
                userLocal,
                parameterMap,
                systemInvoker
        );
    }

    public static CreateMassNotificationRequestDto realtimeEmail(
            String notificationTemplateName,
            String emailAddress,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        return realtimeOneOff(
                notificationTemplateName,
                NotificationChannel.EMAIL,
                emailAddress,
                userLocal,
                parameterMap,
                systemInvoker
        );
    }

    public static void sendMass(KafkaUtil kafkaUtil, CreateMassNotificationRequestDto dto) {
        Objects.requireNonNull(kafkaUtil, "kafkaUtil");
        Objects.requireNonNull(dto, "dto");
        kafkaUtil.send(KafkaTopic.NOTIFICATION_MASS, dto);
    }

    public static void sendRealtimeAppPush(
            KafkaUtil kafkaUtil,
            String notificationTemplateName,
            String fcmToken,
            Locale userLocal,
            Map<String, Object> parameterMap,
            String systemInvoker
    ) {
        sendMass(kafkaUtil, realtimeAppPush(
                notificationTemplateName, fcmToken, userLocal, parameterMap, systemInvoker));
    }

    private static String actionByFrom(Map<String, Object> parameterMap) {
        if (parameterMap == null || parameterMap.get("userId") == null) {
            return null;
        }
        return String.valueOf(parameterMap.get("userId"));
    }
}
