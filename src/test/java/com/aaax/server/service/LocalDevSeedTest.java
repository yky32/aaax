package com.aaax.server.service;

import com.aaax.core.constant.enu.LoginType;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.server.entity.po.user.User;
import com.aaax.server.repository.UserRepository;
import com.aaax.server.support.LoginSmokeAccounts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDevSeedTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private RegisteredClientRepository registeredClientRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LocalDevSeed localDevSeed;

    @Test
    @DisplayName("run should create oauth2 table, client, and primary user when empty")
    void run_shouldSeedClientAndUser() {
        when(registeredClientRepository.findByClientId(LoginSmokeAccounts.OAUTH_CLIENT_ID)).thenReturn(null);
        when(userRepository.findByUsernameIgnoreCase(LoginSmokeAccounts.PRIMARY.canonicalEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        localDevSeed.run();

        verify(jdbcTemplate).execute(LocalDevSeed.OAUTH2_REGISTERED_CLIENT_DDL);
        ArgumentCaptor<RegisteredClient> clientCaptor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(clientCaptor.capture());
        assertEquals(LoginSmokeAccounts.OAUTH_CLIENT_ID, clientCaptor.getValue().getClientId());
        assertTrue(clientCaptor.getValue().getAuthorizationGrantTypes().stream()
                .anyMatch(g -> LoginSmokeAccounts.GRANT_TYPE_CUSTOM_PASSWORD.equals(g.getValue())));
        assertTrue(clientCaptor.getValue().getAuthorizationGrantTypes().stream()
                .anyMatch(g -> "refresh_token".equals(g.getValue())));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(LoginSmokeAccounts.PRIMARY.canonicalEmail(), saved.getUsername());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals(1, saved.getAuthentications().size());
        assertEquals(LoginType.EMAIL, saved.getAuthentications().get(0).getLoginType());
        assertEquals("encoded", saved.getAuthentications().get(0).getCredentials());
    }

    @Test
    @DisplayName("run should skip inserts when client and user already exist")
    void run_shouldSkipWhenPresent() {
        when(registeredClientRepository.findByClientId(LoginSmokeAccounts.OAUTH_CLIENT_ID))
                .thenReturn(RegisteredClient.withId("x")
                        .clientId(LoginSmokeAccounts.OAUTH_CLIENT_ID)
                        .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .build());
        when(userRepository.findByUsernameIgnoreCase(LoginSmokeAccounts.PRIMARY.canonicalEmail()))
                .thenReturn(Optional.of(User.builder().username(LoginSmokeAccounts.PRIMARY.canonicalEmail()).build()));

        localDevSeed.run();

        verify(registeredClientRepository, never()).save(any());
        verify(userRepository, never()).saveAndFlush(any());
    }
}
