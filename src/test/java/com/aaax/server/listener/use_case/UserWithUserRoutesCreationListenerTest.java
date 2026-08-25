package com.aaax.server.listener.use_case;

import com.aaax.core.api.DiscordApiClient;
import com.aaax.core.kafka.event.UserRoutesCreatedEvent;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.server.entity.po.UserRoute;
import com.aaax.server.entity.po.user.Authentication;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRouteRepository;
import com.aaax.server.service.UaaService;
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
class UserWithUserRoutesCreationListenerTest {

    @Mock private UserRouteRepository userRouteRepository;
    @Mock private DiscordApiClient discordApiClient;
    @Mock private UaaService uaaService;
    @Mock private Acknowledgment ack;

    @InjectMocks
    private UserWithUserRoutesCreationListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "webhookId", "wid");
        ReflectionTestUtils.setField(listener, "webhookToken", "wtoken");
    }

    @Test
    @DisplayName("execute should create user route when missing")
    void execute_shouldCreateRoute() {
        User user = User.builder().id(5L).username("u@test.com").build();
        Authentication auth = Authentication.builder().user(user).identifier("u@test.com").build();
        when(uaaService.getByUsername("u@test.com")).thenReturn(auth);
        when(userRouteRepository.findByTenantRoleRouteIdAndUserId(77L, 5L)).thenReturn(Optional.empty());
        when(userRouteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String payload = JSONUtil.writeValue(UserRoutesCreatedEvent.builder()
                .username("u@test.com")
                .tenantRoleRouteId("77")
                .routes(Map.of("home", "/"))
                .build());

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);

            listener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);

            verify(userRouteRepository).save(argThat(r ->
                    r.getUserId().equals(5L) && r.getTenantRoleRouteId().equals(77L)));
        }
    }

    @Test
    @DisplayName("execute should reuse existing user route")
    void execute_shouldReuseExisting() {
        User user = User.builder().id(5L).username("u@test.com").build();
        Authentication auth = Authentication.builder().user(user).identifier("u@test.com").build();
        UserRoute existing = UserRoute.builder().id(1L).userId(5L).tenantRoleRouteId(77L).build();
        when(uaaService.getByUsername("u@test.com")).thenReturn(auth);
        when(userRouteRepository.findByTenantRoleRouteIdAndUserId(77L, 5L)).thenReturn(Optional.of(existing));
        when(userRouteRepository.save(existing)).thenReturn(existing);

        String payload = JSONUtil.writeValue(UserRoutesCreatedEvent.builder()
                .username("u@test.com")
                .tenantRoleRouteId("77")
                .routes(Map.of("home", "/"))
                .build());

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any())).thenAnswer(inv -> null);
            listener.execute(new ConsumerRecord<>("t", 0, 0L, "k", payload), ack);
            verify(userRouteRepository).save(existing);
        }
    }

    @Test
    @DisplayName("elk should swallow discord failures")
    void elk_shouldSwallowFailures() {
        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._void_execute(any()))
                    .thenThrow(new RuntimeException("discord down"));
            listener.elk("{\"ok\":true}");
        }
    }
}
