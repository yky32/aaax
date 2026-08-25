package com.aaax.core.utils;

import com.aaax.core.api.NotificationApiClient;
import com.aaax.core.constant.enu.NotificationChannel;
import com.aaax.core.entity.dto.notification.response.GetNotificationTemplateResponseDto;
import com.aaax.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class NotificationTemplateUtilTest {

    @Test
    void channelsFromSchema_readsTemplatesChannelsOnly_uniqueValid() {
        Object schema = Map.of(
                "channels", List.of("EMAIL"),
                "templates", List.of(Map.of("channels", List.of("APP_PUSH", "EMAIL", "SMS", "NOT_A_CHANNEL")))
        );
        assertEquals(
                List.of(NotificationChannel.APP_PUSH, NotificationChannel.EMAIL, NotificationChannel.SMS),
                NotificationTemplateUtil.channelsFromSchema(schema)
        );
    }

    @Test
    void channelsFromSchema_ignoresTopLevelChannels() {
        Object schema = Map.of("channels", List.of("EMAIL", "SMS"));
        assertTrue(NotificationTemplateUtil.channelsFromSchema(schema).isEmpty());
    }

    @Test
    void channelsFromSchema_returnsEmptyForNullOrMissing() {
        assertTrue(NotificationTemplateUtil.channelsFromSchema(null).isEmpty());
        assertTrue(NotificationTemplateUtil.channelsFromSchema(Map.of("templates", List.of())).isEmpty());
    }

    @Test
    void channelsFromTemplate_usesTemplatesChannels() {
        GetNotificationTemplateResponseDto dto = GetNotificationTemplateResponseDto.builder()
                .schema(Map.of(
                        "templates", List.of(Map.of("channels", List.of("EMAIL", "SMS")))
                ))
                .build();
        assertEquals(
                List.of(NotificationChannel.EMAIL, NotificationChannel.SMS),
                NotificationTemplateUtil.channelsFromTemplate(dto)
        );
    }

    @Test
    void resolveChannels_returnsParsedChannelsFromClient() {
        NotificationApiClient client = mock(NotificationApiClient.class);
        when(client.getByName("cert.approved")).thenReturn(null);

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any())).thenReturn(
                    GetNotificationTemplateResponseDto.builder()
                            .schema(Map.of(
                                    "templates", List.of(Map.of("channels", List.of("APP_PUSH", "EMAIL")))
                            ))
                            .build()
            );
            assertEquals(
                    List.of(NotificationChannel.APP_PUSH, NotificationChannel.EMAIL),
                    NotificationTemplateUtil.resolveChannels(client, "cert.approved")
            );
        }
    }

    @Test
    void resolveChannels_propagatesFetchFailures() {
        NotificationApiClient client = mock(NotificationApiClient.class);
        when(client.getByName("missing")).thenReturn(null);

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler.execute(any()))
                    .thenThrow(new BizException(com.aaax.core.response.SystemResponse.SYS9400, "down"));
            assertThrows(BizException.class, () -> NotificationTemplateUtil.resolveChannels(client, "missing"));
        }

        assertTrue(NotificationTemplateUtil.resolveChannels(null, "x").isEmpty());
        assertTrue(NotificationTemplateUtil.resolveChannels(client, "  ").isEmpty());
    }

    @Test
    void channelsFromTemplate_deserializesIsActiveField() {
        GetNotificationTemplateResponseDto dto = GetNotificationTemplateResponseDto.builder()
                .name("profile.created")
                .isActive(true)
                .schema(Map.of(
                        "templates", List.of(Map.of("channels", List.of("EMAIL")))
                ))
                .build();
        assertEquals(List.of(NotificationChannel.EMAIL), NotificationTemplateUtil.channelsFromTemplate(dto));
        assertEquals(true, dto.getIsActive());
    }
}
