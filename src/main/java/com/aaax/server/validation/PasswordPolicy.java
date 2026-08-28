package com.aaax.server.validation;

import tools.jackson.core.type.TypeReference;
import com.aaax.core.utils.JSONUtil;
import com.aaax.server.config.AaaxSecurityProperties;
import com.aaax.server.entity.po.configuration.SystemConfiguration;
import com.aaax.server.usecase.SystemConfigurationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

    private final AaaxSecurityProperties securityProperties;
    private final SystemConfigurationUseCase systemConfigurationUseCase;

    public List<String> activePatterns() {
        Optional<SystemConfiguration> config =
                systemConfigurationUseCase.getOptionalSystemConfig("USER_CREDENTIALS_REQUIREMENT_REG_EXP", "GLOBAL");
        if (config.isPresent()) {
            List<String> fromDb = JSONUtil.convertFromObject(config.get().getValue(), new TypeReference<>() {});
            if (fromDb != null && !fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return securityProperties.getPasswordPatterns();
    }

    public String encode(PasswordEncoder passwordEncoder, String credentials) {
        return AaaxValidation.check_passwordRequirement(passwordEncoder, credentials, activePatterns());
    }
}
