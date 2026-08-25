package com.aaax.server.endpoint.api;

import com.aaax.core.entity.dto.uaa.response.GetUserResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.RegisterUserRequestDto;
import com.aaax.server.entity.dto.request.UserIdentityVerificationResultRequestDto;
import com.aaax.server.entity.dto.response.PendingVerifyUserResponseDto;
import com.aaax.server.usecase.RegisterUserUseCase;
import com.aaax.server.usecase.UserIdentityVerificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicUserRegistrationEndpointTest {

    @Mock private RegisterUserUseCase registerUserUseCase;
    @Mock private UserIdentityVerificationUseCase userIdentityVerificationUseCase;

    @InjectMocks
    private PublicUserRegistrationEndpoint endpoint;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(endpoint, "systemInvoker", "QS");
    }

    @Test
    @DisplayName("register should validate and return pending dto")
    void register_shouldDelegate() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder().username("u@test.com").build();
        PendingVerifyUserResponseDto pending = PendingVerifyUserResponseDto.builder().username("u@test.com").build();
        when(registerUserUseCase.register_public(any())).thenReturn(pending);

        Result<PendingVerifyUserResponseDto> result = endpoint.register(request, null, null);

        assertEquals("u@test.com", result.getData().getUsername());
        assertEquals("QS", request.getSourceSystem());
        verify(registerUserUseCase).registerValidations(request);
        verify(registerUserUseCase).register_public(request);
        verify(registerUserUseCase, never()).register_public_checkOnly(any());
    }

    @Test
    @DisplayName("register?check=1 should availability-check only")
    void register_checkOnly_shouldDelegate() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder().username("free@test.com").build();
        PendingVerifyUserResponseDto ok = PendingVerifyUserResponseDto.builder().username("free@test.com").build();
        when(registerUserUseCase.register_public_checkOnly(any())).thenReturn(ok);

        Result<PendingVerifyUserResponseDto> result = endpoint.register(request, null, "1");

        assertEquals("free@test.com", result.getData().getUsername());
        verify(registerUserUseCase).register_public_checkOnly(request);
        verify(registerUserUseCase, never()).register_public(any());
    }

    @Test
    @DisplayName("verifyRegister should delegate")
    void verifyRegister_shouldDelegate() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder().build();
        when(registerUserUseCase.verify(request)).thenReturn(true);
        assertTrue(endpoint.verifyRegister(request).getData());
    }

    @Test
    @DisplayName("create should reject phone metadata without areaCode")
    void create_shouldValidatePhoneMetadata() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder()
                .metadata(new HashMap<>(Map.of("phone", "91234567")))
                .build();
        assertThrows(BizException.class, () -> endpoint.create(request, null, null));
        verify(registerUserUseCase, never()).execute_external(any());
    }

    @Test
    @DisplayName("create should set extra features and create user")
    void create_shouldCreateUser() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder().username("u@test.com").build();
        GetUserResponseDto user = GetUserResponseDto.builder().id("u_1").username("u@test.com").build();
        when(registerUserUseCase.execute_external(any())).thenReturn(user);

        Result<GetUserResponseDto> result = endpoint.create(request, List.of("ONBOARDING"), "APP");

        assertEquals("u_1", result.getData().getId());
        verify(registerUserUseCase).execute_external(argThat(r ->
                r.getExtraFeatures() != null && r.getExtraFeatures().contains("ONBOARDING")));
    }

    @Test
    @DisplayName("idvResults should delegate to use case")
    void idvResults_shouldDelegate() {
        UserIdentityVerificationResultRequestDto request = UserIdentityVerificationResultRequestDto.builder()
                .accountId("a").workflowExecutionId("w").build();
        assertNotNull(endpoint.idvResults(request, null));
        verify(userIdentityVerificationUseCase).updateIdvResults(argThat(r -> "QS".equals(r.getSourceSystem())));
    }

    @Test
    @DisplayName("register_external_noOtp should execute register use case")
    void registerExternal_shouldExecute() {
        RegisterUserRequestDto request = RegisterUserRequestDto.builder().username("e@test.com").build();
        when(registerUserUseCase.execute(request)).thenReturn(GetUserResponseDto.builder().id("u_2").build());
        assertEquals("u_2", endpoint.register_external_noOtp(request, null).getData().getId());
    }
}
