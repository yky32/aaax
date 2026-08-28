package com.aaax.core.security;

import com.aaax.core.utils.JwtUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Slf4j
@Builder
@Data
public class AaaxAuditAware implements AuditorAware<String> {

    private String defaultUsername;

    public AaaxAuditAware(String defaultUsername) {
        if (defaultUsername == null) {
            throw new IllegalArgumentException("Please Specify the [defaultUsername] in [AaaxAuditAware].");
        }
        this.defaultUsername = defaultUsername;
    }

    @Override
    public @NotNull Optional<String> getCurrentAuditor() {
        String userId;
        try {
            userId = JwtUtil.userId();
        } catch (Exception exception) {
            log.info("-- Error in  getCurrentAuditor : {}", "JwtUtil.USER_ID");
            userId = null;
        }

        if (userId == null || StringUtils.isEmpty(userId)) {
            return Optional.of(defaultUsername);
        }
        return Optional.of(userId);
    }
}
