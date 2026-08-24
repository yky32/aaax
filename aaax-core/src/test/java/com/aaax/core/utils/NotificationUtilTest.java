package com.aaax.core.utils;

import com.aaax.core.constant.enu.Locale;
import com.aaax.core.constant.enu.NotificationAction;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.constant.enu.NotificationFrequency;
import com.aaax.core.kafka.CreateMassNotificationRequestDto;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationUtilTest {

    @Test
    void realtimeAppPush_buildsRealtimeOneOffPayload() {
        CreateMassNotificationRequestDto dto = NotificationUtil.realtimeAppPush(
                "certificate.document.approved",
                "fcm-token",
                Locale.zh_TW,
                Map.of("userId", "u-1", "certification", "CSCS"),
                "TGT"
        );

        assertEquals("certificate.document.approved", dto.getNotificationTemplateName());
        assertEquals(List.of(NotificationChannel.APP_PUSH), dto.getChannels());
        assertEquals(NotificationAction.REALTIME, dto.getNotificationAction());
        assertEquals(NotificationFrequency.ONE_OFF, dto.getNotificationFrequency());
        assertEquals(Locale.zh_TW, dto.getUserLocal());
        assertEquals(List.of(Locale.EN, Locale.zh_TW), dto.getLocale());
        assertEquals("TGT", dto.getSystemInvoker());
        assertEquals("u-1", dto.getActionBy());
        assertEquals(1, dto.getRecipients().size());
        assertEquals("fcm-token", dto.getRecipients().get(0).getTo());
        assertEquals(Locale.zh_TW, dto.getRecipients().get(0).getUserLocal());
    }

    @Test
    void realtimeOneOff_defaultsLocaleToEnAndSkipsBlankRecipients() {
        CreateMassNotificationRequestDto dto = NotificationUtil.realtimeOneOff(
                "tpl",
                NotificationChannel.SMS,
                Arrays.asList(" ", "85290001111", "85290001111", null),
                null,
                Map.of("k", "v"),
                "TGT"
        );

        assertEquals(Locale.EN, dto.getUserLocal());
        assertEquals(List.of(Locale.EN), dto.getLocale());
        assertEquals(1, dto.getRecipients().size());
        assertEquals("85290001111", dto.getRecipients().get(0).getTo());
        assertNull(dto.getActionBy());
    }

    @Test
    void realtimeAppPush_multiRecipient_keepsDistinctTokens() {
        CreateMassNotificationRequestDto dto = NotificationUtil.realtimeAppPush(
                "tpl",
                List.of("t1", "t1", "t2"),
                Locale.EN,
                Map.of("userId", "42"),
                "TGT"
        );

        assertEquals(2, dto.getRecipients().size());
        assertEquals("42", dto.getActionBy());
    }

    @Test
    void realtimeEmail_usesEmailChannel() {
        CreateMassNotificationRequestDto dto = NotificationUtil.realtimeEmail(
                "otp.email",
                "a@b.com",
                Locale.EN,
                Map.of("userId", "u"),
                "UAA"
        );

        assertEquals(List.of(NotificationChannel.EMAIL), dto.getChannels());
        assertTrue(dto.getRecipients().stream().anyMatch(r -> "a@b.com".equals(r.getTo())));
    }
}
