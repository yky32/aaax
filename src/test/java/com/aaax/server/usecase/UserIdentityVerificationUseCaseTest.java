package com.aaax.server.usecase;

import com.aaax.core.constant.enu.UserVerificationStatus;
import com.aaax.core.entity.dto.uaa.response.GetUserMetricsResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserPreferenceResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserProfileResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.entity.dto.uaa.response.GetUserVerificationResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.KafkaUtil;
import com.aaax.core.utils.RetrofitCallHandler;
import com.aaax.server.entity.dto.request.UserIdentityVerificationResultRequestDto;
import com.aaax.server.entity.dto.response.GetSystemConfigurationRequestDto;
import com.aaax.server.entity.enu.UserProfileType;
import com.aaax.server.entity.po.user_management.UserProfile;
import com.aaax.server.entity.po.user_verification.UserVerification;
import com.aaax.server.ext.api.client.idv.IdvApiClient;
import com.aaax.server.ext.api.client.idv.dto.CreateIdvResponseDto;
import com.aaax.server.repository.UserProfileRepository;
import com.aaax.server.repository.UserVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserIdentityVerificationUseCaseTest {

    @Mock private IdvApiClient idvApiClient;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserVerificationRepository userVerificationRepository;
    @Mock private SystemConfigurationUseCase systemConfigurationUseCase;
    @Mock private UserMetricsUseCase userMetricsUseCase;
    @Mock private KafkaUtil kafkaUtil;

    @InjectMocks
    private UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userIdentityVerificationUseCase, "uaaWebhookDomain", "https://uaa/");
        ReflectionTestUtils.setField(userIdentityVerificationUseCase, "timezone", "UTC");
    }

    @Test
    @DisplayName("updateIdvResults should create verification pending callback")
    void updateIdvResults_shouldCreatePendingCallback() {
        UserProfile profile = UserProfile.builder()
                .id(1L).userId(10L)
                .context(Map.of("verification", Map.of("idvStatus", "PENDING")))
                .build();
        when(userProfileRepository.findByUserIdAndType(10L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class);
             MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            retrofit.when(() -> RetrofitCallHandler.execute(any()))
                    .thenReturn(CreateIdvResponseDto.builder().id("idv-1").build());

            userIdentityVerificationUseCase.updateIdvResults(
                    UserIdentityVerificationResultRequestDto.builder()
                            .accountId("acc")
                            .workflowExecutionId("wf")
                            .sourceSystem("APP")
                            .build());

            verify(userVerificationRepository).save(argThat(uv ->
                    uv.getStatus() == UserVerificationStatus.PENDING_CALLBACK
                            && "idv-1".equals(uv.getExtIdentifier())));
        }
    }

    @Test
    @DisplayName("updateIdvResults should reject suspended profile")
    void updateIdvResults_shouldRejectSuspended() {
        UserProfile profile = UserProfile.builder()
                .id(1L).userId(10L)
                .context(Map.of("verification", Map.of("idvStatus", "SUSPENDED")))
                .build();
        when(userProfileRepository.findByUserIdAndType(10L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(JwtUtil::userId).thenReturn("10");
            assertThrows(BizException.class, () -> userIdentityVerificationUseCase.updateIdvResults(
                    UserIdentityVerificationResultRequestDto.builder().build()));
        }
    }

    @Test
    @DisplayName("complete should mark verified when liveness and similarity passed")
    void complete_shouldMarkVerified() {
        UserVerification verification = UserVerification.builder()
                .id(1L).userId(10L).extIdentifier("idv-1")
                .detail(new HashMap<>(Map.of("sourceSystem", "APP")))
                .status(UserVerificationStatus.PENDING_CALLBACK)
                .build();
        when(userVerificationRepository.findByUserIdAndExtIdentifier(10L, "idv-1"))
                .thenReturn(Optional.of(verification));
        when(userVerificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(systemConfigurationUseCase.query("IDV_FAILURE_COUNTER", "APP"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(5).build());
        when(userVerificationRepository.findByUserId(10L)).thenReturn(List.of(verification));

        UserProfile profile = UserProfile.builder()
                .id(2L).userId(10L)
                .context(new HashMap<>(Map.of("verification", new HashMap<>(Map.of("idvStatus", "PENDING")))))
                .build();
        when(userProfileRepository.findByUserIdAndType(10L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GetUserProfileResponseDto result = userIdentityVerificationUseCase.complete("u_10", Map.of(
                "id", "idv-1",
                "status", "CLOSED",
                "detail", Map.of("liveness", "PASSED", "similarity", "PASSED")
        ));

        assertEquals(UserVerificationStatus.VERIFIED, verification.getStatus());
        assertNotNull(result);
    }

    @Test
    @DisplayName("complete should bypass when already verified")
    void complete_shouldBypassWhenAlreadyVerified() {
        when(userVerificationRepository.findByUserIdAndExtIdentifier(10L, "idv-1"))
                .thenReturn(Optional.empty());
        when(systemConfigurationUseCase.query("IDV_FAILURE_COUNTER", "GLOBAL"))
                .thenReturn(GetSystemConfigurationRequestDto.builder().value(5).build());
        when(userVerificationRepository.findByUserId(10L)).thenReturn(List.of());
        UserProfile profile = UserProfile.builder()
                .id(2L).userId(10L)
                .context(Map.of("verification", Map.of("idvStatus", "VERIFIED")))
                .build();
        when(userProfileRepository.findByUserIdAndType(10L, UserProfileType.DEFAULT.name()))
                .thenReturn(Optional.of(profile));

        GetUserProfileResponseDto result = userIdentityVerificationUseCase.complete("u_10", Map.of("id", "idv-1"));
        assertEquals("up_2", result.getId());
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("patchStatuses should update and return verification")
    void patchStatuses_shouldUpdate() {
        UserVerification verification = UserVerification.builder()
                .id(5L).userId(1L).status(UserVerificationStatus.PENDING).detail(Map.of()).build();
        when(userVerificationRepository.findById(5L)).thenReturn(Optional.of(verification));
        when(userVerificationRepository.save(verification)).thenReturn(verification);

        GetUserVerificationResponseDto result = userIdentityVerificationUseCase.patchStatuses(
                "uv_5", UserVerificationStatus.VERIFIED, "APP");

        assertEquals(UserVerificationStatus.VERIFIED, verification.getStatus());
        assertEquals("uv_5", result.getId());
    }

    @Test
    @DisplayName("getOne should return verification or throw")
    void getOne_shouldReturnOrThrow() {
        UserVerification verification = UserVerification.builder()
                .id(5L).userId(1L).status(UserVerificationStatus.PENDING).detail(Map.of()).build();
        when(userVerificationRepository.findById(5L)).thenReturn(Optional.of(verification));
        assertEquals("uv_5", userIdentityVerificationUseCase.getOne("uv_5").getId());

        when(userVerificationRepository.findById(6L)).thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> userIdentityVerificationUseCase.getOne("uv_6"));
    }

    @Test
    @DisplayName("myVerifications should sort by createDt descending")
    void myVerifications_shouldSort() {
        UserVerification older = UserVerification.builder().id(1L).userId(1L).status(UserVerificationStatus.PENDING).detail(Map.of()).build();
        older.setCreateDt(Instant.parse("2024-01-01T00:00:00Z"));
        UserVerification newer = UserVerification.builder().id(2L).userId(1L).status(UserVerificationStatus.VERIFIED).detail(Map.of()).build();
        newer.setCreateDt(Instant.parse("2024-02-01T00:00:00Z"));
        when(userVerificationRepository.findByUserId(1L)).thenReturn(List.of(older, newer));

        List<GetUserVerificationResponseDto> result = userIdentityVerificationUseCase.myVerifications("u_1");
        assertEquals("uv_2", result.get(0).getId());
    }

    @Test
    @DisplayName("afterVerification should notify rentease verified users")
    void afterVerification_shouldNotifyRentease() {
        GetUserMetricsResponseDto metrics = GetUserMetricsResponseDto.builder()
                .user(GetUserResponseDto.builder().id("u_1").build())
                .profile(GetUserProfileResponseDto.builder().context(Map.of("email", "a@b.com", "firstName", "A", "lastName", "B")).build())
                .preference(GetUserPreferenceResponseDto.builder().context(Map.of(
                        "localizations", Map.of("selected", "en"),
                        "notifications", Map.of("platforms", Map.of("all", List.of(
                                Map.of("name", "APP_PUSH", "isEnabled", true)
                        )))
                )).build())
                .device(com.aaax.core.entity.dto.uaa.response.GetUserDeviceResponseDto.builder()
                        .context(List.of(com.aaax.core.common.jsonfield.DeviceMetadata.builder()
                                .token(Map.of("fcm", "token-1")).build()))
                        .build())
                .build();
        when(userMetricsUseCase.execute("u_1", "RENTEASE")).thenReturn(metrics);

        userIdentityVerificationUseCase.afterVerification("u_1", "VERIFIED", "RENTEASE");

        verify(kafkaUtil).send(anyString(), any());
    }
}
