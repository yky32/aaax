package com.aaax.account;

import java.time.Instant;
import java.util.Set;

public record AccountResponse(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.roleSet(),
                account.isEnabled(),
                account.getCreatedAt());
    }
}
