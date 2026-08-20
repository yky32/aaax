package com.aaax.account;

import java.time.Instant;

public record AccountResponse(
        String id,
        String username,
        String email,
        boolean enabled,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.isEnabled(),
                account.getCreatedAt());
    }
}
