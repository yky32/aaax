package com.aaax.auth.application;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.account.Account;
import com.aaax.account.application.PasswordUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Component;

@Component
public class PasswordLoginUseCase {

    public static final String MFA_PENDING_USER = "AAAX_MFA_PENDING_USER";

    private final PasswordUseCase passwords;
    private final FinishAuthenticatedSession finish;

    public PasswordLoginUseCase(PasswordUseCase passwords, FinishAuthenticatedSession finish) {
        this.passwords = passwords;
        this.finish = finish;
    }

    public Map<String, Object> execute(
            PasswordLoginCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        Account account = passwords.authenticatePassword(cmd.username(), cmd.password());
        if (account.isTotpEnabled()) {
            HttpSession session = request.getSession(true);
            session.setAttribute(MFA_PENDING_USER, account.getUsername());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mfaRequired", true);
            m.put("mfaType", "totp");
            m.put("username", account.getUsername());
            return m;
        }
        return finish.execute(account, "password", request, response, true);
    }

    public record PasswordLoginCommand(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password
    ) {
    }
}
