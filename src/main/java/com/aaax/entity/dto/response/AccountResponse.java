package com.aaax.entity.dto.response;

import java.time.Instant;
import java.util.Set;
import com.aaax.entity.po.Account;

public record AccountResponse(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean mfaEnabled,
        boolean googleLinked,
        boolean githubLinked,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.roleSet(),
                account.isEnabled(),
                account.isTotpEnabled(),
                account.getGoogleSub() != null && !account.getGoogleSub().isBlank(),
                account.getGithubId() != null && !account.getGithubId().isBlank(),
                account.getCreatedAt());
    }
}
