package com.aaax.entity.dto.response;

import java.time.Instant;
import java.util.Set;

import com.aaax.entity.po.Account;

/** Account public shape (qs/uaa: Get*ResponseDto). */
public record GetAccountResponseDto(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean mfaEnabled,
        boolean googleLinked,
        boolean githubLinked,
        Instant createDt
) {
    public static GetAccountResponseDto from(Account account) {
        return new GetAccountResponseDto(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.roleSet(),
                account.isEnabled(),
                account.isTotpEnabled(),
                account.getGoogleSub() != null && !account.getGoogleSub().isBlank(),
                account.getGithubId() != null && !account.getGithubId().isBlank(),
                account.getCreateDt());
    }
}
