package com.aaax.server.usecase;

import com.aaax.core.exception.BizException;
import com.aaax.server.entity.dto.request.CreateRegisteredClientRequestDto;
import com.aaax.server.entity.dto.response.ClientResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UaaSettingUseCaseTest {

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RegisteredClientRepository registeredClientRepository;

    @InjectMocks
    private UaaSettingUseCase uaaSettingUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uaaSettingUseCase, "serverTokenExpiryTime", 3600);
        ReflectionTestUtils.setField(uaaSettingUseCase, "registeredClientSecret", "secret");
    }

    @Test
    @DisplayName("createRegisteredClient should return existing client")
    void createRegisteredClient_shouldReturnExisting() {
        RegisteredClient existing = RegisteredClient.withId("id-1").clientId("c1")
                .clientSecret("x")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(registeredClientRepository.findByClientId("c1")).thenReturn(existing);

        assertSame(existing, uaaSettingUseCase.createRegisteredClient(
                CreateRegisteredClientRequestDto.builder().clientId("c1").tokenExpiryTime(60).build()));
        verify(registeredClientRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRegisteredClient should save new client")
    void createRegisteredClient_shouldSaveNew() {
        when(registeredClientRepository.findByClientId("c2")).thenReturn(null);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        RegisteredClient result = uaaSettingUseCase.createRegisteredClient(
                CreateRegisteredClientRequestDto.builder().clientId("c2").tokenExpiryTime(120).build());

        assertEquals("c2", result.getClientId());
        verify(registeredClientRepository).save(any(RegisteredClient.class));
    }

    @Test
    @DisplayName("create should return existing via search")
    void create_shouldReturnExisting() {
        RegisteredClient existing = RegisteredClient.withId("id-1").clientId("c1")
                .clientSecret("x")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(registeredClientRepository.findByClientId("c1")).thenReturn(existing);
        when(registeredClientRepository.findById("id-1")).thenReturn(existing);

        ClientResponseDto result = uaaSettingUseCase.create(
                CreateRegisteredClientRequestDto.builder().clientId("c1").tokenExpiryTime(60).build());

        assertEquals("c1", result.getClientId());
        assertNull(result.getClientSecret());
    }

    @Test
    @DisplayName("create should generate secret for new client")
    void create_shouldGenerateSecret() {
        when(registeredClientRepository.findByClientId("c3")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ClientResponseDto result = uaaSettingUseCase.create(
                CreateRegisteredClientRequestDto.builder().clientId("c3").clientName("name").tokenExpiryTime(60).build());

        assertEquals("c3", result.getClientId());
        assertNotNull(result.getClientSecret());
        verify(registeredClientRepository).save(any());
    }

    @Test
    @DisplayName("search should map registered client")
    void search_shouldMap() {
        RegisteredClient existing = RegisteredClient.withId("id-9").clientId("c9")
                .clientSecret("x")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(registeredClientRepository.findById("id-9")).thenReturn(existing);

        ClientResponseDto result = uaaSettingUseCase.search("id-9");
        assertEquals("id-9", result.getId());
        assertEquals("c9", result.getClientId());
    }

    @Test
    @DisplayName("updateSecret should throw when client missing")
    void updateSecret_shouldThrowWhenMissing() {
        when(registeredClientRepository.findById("missing")).thenReturn(null);
        assertThrows(BizException.class, () -> uaaSettingUseCase.updateSecret("missing"));
    }

    @Test
    @DisplayName("updateSecret should rotate secret")
    void updateSecret_shouldRotate() {
        RegisteredClient existing = RegisteredClient.withId("id-1").clientId("c1")
                .clientSecret("old")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(registeredClientRepository.findById("id-1")).thenReturn(existing);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-new");

        ClientResponseDto result = uaaSettingUseCase.updateSecret("id-1");

        assertEquals("c1", result.getClientId());
        assertNotNull(result.getClientSecret());
        verify(registeredClientRepository).save(any());
    }

    @Test
    @DisplayName("getBasicAuthorization should encode client:secret")
    void getBasicAuthorization_shouldEncode() {
        RegisteredClient existing = RegisteredClient.withId("id-1").clientId("c1")
                .clientSecret("x")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        when(registeredClientRepository.findById("id-1")).thenReturn(existing);

        String encoded = uaaSettingUseCase.getBasicAuthorization("id-1");
        assertEquals(Base64.getEncoder().encodeToString("c1:secret".getBytes()), encoded);
    }

    @Test
    @DisplayName("getBasicAuthorization should throw when missing")
    void getBasicAuthorization_shouldThrowWhenMissing() {
        when(registeredClientRepository.findById("x")).thenReturn(null);
        assertThrows(BizException.class, () -> uaaSettingUseCase.getBasicAuthorization("x"));
    }
}
