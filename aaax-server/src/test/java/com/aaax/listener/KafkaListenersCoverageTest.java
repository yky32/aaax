package com.aaax.listener;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.kafka.event.UserAliasGeneratedEvent;
import com.aaax.core.kafka.event.UserDeletedEvent;
import com.aaax.core.kafka.event.UserPermissionMutatedEvent;
import com.aaax.core.kafka.event.UserStateMutatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.config.security.RedisOAuth2AuthorizationService;
import com.aaax.entity.po.user.User;
import com.aaax.repository.UserRepository;
import com.aaax.service.UaaService;
import com.aaax.usecase.AccessControlUseCase;
import com.aaax.usecase.RegisterUserUseCase;
import com.aaax.usecase.UserManagementUseCase;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaListenersCoverageTest {

    @Mock private DiscordApiClient discordApiClient;
    @Mock private RedisOAuth2AuthorizationService redisOauth2AuthorizationService;
    @Mock private UaaService uaaService;
    @Mock private UserRepository userRepository;
    @Mock private AccessControlUseCase accessControlUseCase;
    @Mock private UserManagementUseCase userManagementUseCase;
    @Mock private RegisterUserUseCase registerUserUseCase;
    @Mock private KafkaUtil kafkaUtil;
    @Mock private com.aaax.core.utils.RedisUtil redisUtil;
    @Mock private Acknowledgment ack;

    @InjectMocks private UserStatusChangeListener userStatusChangeListener;
    @InjectMocks private UserStateChangeListener userStateChangeListener;
    @InjectMocks private UserPermissionMutationListener userPermissionMutationListener;
    @InjectMocks private UserDeletedNotificationListener userDeletedNotificationListener;
    @InjectMocks private UserAliasGeneratedListener userAliasGeneratedListener;

    @BeforeEach
    void setUp() {
        for (Object listener : List.of(
                userStatusChangeListener, userStateChangeListener, userPermissionMutationListener,
                userDeletedNotificationListener, userAliasGeneratedListener)) {
            ReflectionTestUtils.setField(listener, "webhookId", "wid");
            ReflectionTestUtils.setField(listener, "webhookToken", "wtoken");
        }
        ReflectionTestUtils.setField(userDeletedNotificationListener, "systemInvoker", "QS");
    }

    @Test
    @DisplayName("UserStatusChangeListener should update status and notify discord")
    void statusChange_shouldUpdateAndNotify() {
        String payload = JSONUtil.writeValue(UserStateMutatedEvent.builder()
                .userId("u_1").toBeStatus(UserStatus.SUSPENDED).build());
        when(uaaService.getById("u_1")).thenReturn(User.builder().id(1L).status(UserStatus.ACTIVE).build());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);
            userStatusChangeListener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
            verify(redisOauth2AuthorizationService).cleanUp("1");
            verify(userRepository).save(argThat(u -> u.getStatus() == UserStatus.SUSPENDED));
        }
    }

    @Test
    @DisplayName("UserStateChangeListener should cleanup oauth and notify discord")
    void stateChange_shouldCleanup() {
        String payload = JSONUtil.writeValue(UserStateMutatedEvent.builder().userId("u_2").build());
        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);
            userStateChangeListener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
            verify(redisOauth2AuthorizationService).cleanUp("2");
        }
    }

    @Test
    @DisplayName("UserPermissionMutationListener should assign permissions")
    void permissionMutation_shouldAssign() {
        String payload = JSONUtil.writeValue(UserPermissionMutatedEvent.builder().userId("u_3").build());
        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);
            userPermissionMutationListener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
            verify(accessControlUseCase).assignPermissionToUser(any());
        }
    }

    @Test
    @DisplayName("UserDeletedNotificationListener should hard-delete user")
    void deleted_shouldHardDelete() {
        String payload = JSONUtil.writeValue(UserDeletedEvent.builder().userId("u_4").build());
        userDeletedNotificationListener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
        verify(userManagementUseCase).deleteByUserId("u_4", false);
    }

    @Test
    @DisplayName("UserAliasGeneratedListener should post discord webhook")
    void aliasGenerated_shouldNotify() {
        String payload = JSONUtil.writeValue(UserAliasGeneratedEvent.builder().userId("u_5").build());
        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);
            userAliasGeneratedListener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
            retrofit.verify(() -> RetrofitCallHandler._void_execute(any()));
        }
    }
}
