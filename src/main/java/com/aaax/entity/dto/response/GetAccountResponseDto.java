package com.aaax.entity.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.aaax.core.entity.dto.BaseResponseDto;
import com.aaax.entity.po.Account;

/**
 * Account public shape (qs/uaa: Get*ResponseDto).
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
        List<String> linkedProviders,
        Instant createDt,
        Instant updateDt,
        String createdBy,
        String updatedBy,
        Boolean isActive) {

    public static GetAccountResponseDto from(Account account) {
        return from(account, List.of());
    }

    public static GetAccountResponseDto from(Account account, List<String> linkedProviders) {
        BaseResponseDto audit = BaseResponseDto.from(account);
        List<String> linked = linkedProviders == null ? List.of() : List.copyOf(linkedProviders);
        boolean google = linked.contains("google")
                || (account.getGoogleSub() != null && !account.getGoogleSub().isBlank());
        boolean github = linked.contains("github")
                || (account.getGithubId() != null && !account.getGithubId().isBlank());
        return new GetAccountResponseDto(
                account.getId(),
                account.getUsername(),
                account.getEmail(),
                account.roleSet(),
                account.isEnabled(),
                account.isTotpEnabled(),
                google,
                github,
                linked,
                audit != null ? audit.createDt() : null,
                audit != null ? audit.updateDt() : null,
                audit != null ? audit.createdBy() : null,
                audit != null ? audit.updatedBy() : null,
                audit != null ? audit.isActive() : null);
    }
}
