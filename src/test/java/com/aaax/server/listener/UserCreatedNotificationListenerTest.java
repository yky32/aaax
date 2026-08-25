package com.aaax.server.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.kafka.event.UserCreatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCreatedNotificationListenerTest {

    @Mock private KafkaUtil kafkaUtil;
    @Mock private DiscordApiClient discordApiClient;
    @Mock private SystemConfigurationUseCase systemConfigurationUseCase;
    @Mock private UserRepository userRepository;
    @Mock private Acknowledgment ack;

    @InjectMocks
    private UserCreatedNotificationListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "systemInvoker", "QS");
        ReflectionTestUtils.setField(listener, "webhookId", "wid");
        ReflectionTestUtils.setField(listener, "webhookToken", "wtoken");
    }

    @Test
    @DisplayName("execute should skip when user already exists")
    void execute_shouldSkipExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        String payload = JSONUtil.writeValue(UserCreatedEvent.builder()
                .userId(1L).username("u@test.com").build());

        listener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);

        verify(kafkaUtil, never()).send(anyString(), any());
        verifyNoInteractions(systemConfigurationUseCase);
    }

    @Test
    @DisplayName("execute should send mass notification and discord webhook")
    void execute_shouldNotify() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(systemConfigurationUseCase.query("NOTIFICATION", "USER_CREATED_EVENT_TEMPLATE_ID"))
                .thenReturn(GetSystemConfigurationRequestDto.builder()
                        .value(Map.of(
                                "templateId", "tpl-1",
                                "loginUrl", "https://login",
                                "notificationTemplateName", "USER_CREATED"
                        ))
                        .build());

        String payload = JSONUtil.writeValue(UserCreatedEvent.builder()
                .userId(2L).username("new@test.com").build());

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);

            listener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);

            verify(kafkaUtil).send(anyString(), any());
            retrofit.verify(() -> RetrofitCallHandler._void_execute(any()));
        }
    }
}
