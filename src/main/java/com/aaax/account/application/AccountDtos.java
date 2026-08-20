package com.aaax.account.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HTTP/application DTOs for account flows (moved off God AccountService). */
public final class AccountDtos {

    private AccountDtos() {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Size(max = 320) String usernameOrEmail
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(min = 4, max = 10) String code,
            @NotBlank @Size(min = 8, max = 128) String newPassword
    ) {
    }

    public record SetEnabledRequest(boolean enabled) {
    }

    public record SetRolesRequest(@NotBlank String roles) {
    }

    public record TotpSetupResponse(String secret, String otpauthUrl) {
    }

    public record TotpCodeRequest(
            @NotBlank @Size(min = 6, max = 6) String code,
            Boolean rememberDevice,
            @Size(max = 128) String deviceLabel) {
        public TotpCodeRequest(String code) {
            this(code, null, null);
        }
    }

    public record DisableTotpRequest(
            @NotBlank String password,
            @NotBlank @Size(min = 6, max = 6) String code
    ) {
    }

    public record BootstrapAdminRequest(
            @NotBlank @Size(max = 64) String username,
            @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            String bootstrapToken
    ) {
    }
}
