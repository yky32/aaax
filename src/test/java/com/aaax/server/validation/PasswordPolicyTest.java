package com.aaax.server.validation;

import com.aaax.core.exception.BizException;
import com.aaax.server.config.AaaxSecurityProperties;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordPolicyTest {

    @Mock private SystemConfigurationUseCase systemConfigurationUseCase;
    @Mock private PasswordEncoder passwordEncoder;

    private PasswordPolicy passwordPolicy;

    @BeforeEach
    void setUp() {
        AaaxSecurityProperties props = new AaaxSecurityProperties();
        passwordPolicy = new PasswordPolicy(props, systemConfigurationUseCase);
    }

    @Test
    @DisplayName("encode should reject passwords shorter than yaml default")
    void encode_shouldRejectShortPassword() {
        when(systemConfigurationUseCase.getOptionalSystemConfig("USER_CREDENTIALS_REQUIREMENT_REG_EXP", "GLOBAL"))
                .thenReturn(Optional.empty());
        assertThrows(BizException.class, () -> passwordPolicy.encode(passwordEncoder, "short"));
    }

    @Test
    @DisplayName("encode should use system config regex when present")
    void encode_shouldUseDbPatterns() {
        SystemConfiguration config = SystemConfiguration.builder()
                .value(List.of(".*[A-Z].*"))
                .build();
        when(systemConfigurationUseCase.getOptionalSystemConfig("USER_CREDENTIALS_REQUIREMENT_REG_EXP", "GLOBAL"))
                .thenReturn(Optional.of(config));
        when(passwordEncoder.encode("HasUpper1")).thenReturn("enc");
        assertEquals("enc", passwordPolicy.encode(passwordEncoder, "HasUpper1"));
        assertThrows(BizException.class, () -> passwordPolicy.encode(passwordEncoder, "noupper1"));
    }
}
