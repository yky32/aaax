package com.aaax.entity.dto.response;

import java.time.Instant;
import java.util.Set;

import com.aaax.core.entity.dto.BaseResponseDto;
import com.aaax.entity.po.Account;

/**
 * Account public shape (qs/uaa: Get*ResponseDto).
 * Audit fields via reusable {@link BaseResponseDto}.
 */
public record GetAccountResponseDto(
        String id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean mfaEnabled,
        boolean googleLinked,
        boolean githubLinked,
        Instant createDt,
        Instant updateDt,
        String createdBy,
        String updatedBy,
        Boolean isActive
) {
    public static GetAccountResponseDto from(Account account) {
        BaseResponseDto audit = BaseResponseDto.from(account);
        return new GetAccountResponseDto(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.roleSet(),
                account.isEnabled(),
                account.isTotpEnabled(),
                account.getGoogleSub() != null && !account.getGoogleSub().isBlank(),
                account.getGithubId() != null && !account.getGithubId().isBlank(),
                audit != null ? audit.createDt() : null,
                audit != null ? audit.updateDt() : null,
                audit != null ? audit.createdBy() : null,
                audit != null ? audit.updatedBy() : null,
                audit != null ? audit.isActive() : null);
    }
}
