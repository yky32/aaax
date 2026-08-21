package com.aaax.usecase.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import com.aaax.entity.po.Account;
import com.aaax.usecase.account.PasswordUseCase;
import com.aaax.service.TrustedDeviceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Component;

@Component
public class PasswordLoginUseCase {

    public static final String MFA_PENDING_USER = "AAAX_MFA_PENDING_USER";
    public static final String MFA_REMEMBER_DEVICE = "AAAX_MFA_REMEMBER_DEVICE";

    private final PasswordUseCase passwords;
    private final FinishAuthenticatedSession finish;
    private final TrustedDeviceService devices;

    public PasswordLoginUseCase(
            PasswordUseCase passwords, FinishAuthenticatedSession finish, TrustedDeviceService devices) {
        this.passwords = passwords;
        this.finish = finish;
        this.devices = devices;
    }

    public Map<String, Object> execute(
            PasswordLoginCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        Account account = passwords.authenticatePassword(cmd.username(), cmd.password());
        boolean remember = Boolean.TRUE.equals(cmd.rememberDevice());
        String rawDevice = devices.readRawToken(request).orElse(null);

        if (account.isTotpEnabled()) {
            if (rawDevice != null && devices.isTrusted(account.getId(), rawDevice)) {
                devices.touch(account.getId(), rawDevice, request);
                Map<String, Object> m =
                        finish.execute(account, "password+trusted-device", request, response, true);
                m.put("trustedDevice", true);
                m.put("mfaSkipped", true);
                return m;
            }
            HttpSession session = request.getSession(true);
            session.setAttribute(MFA_PENDING_USER, account.getUsername());
            session.setAttribute(MFA_REMEMBER_DEVICE, remember);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mfaRequired", true);
            m.put("mfaType", "totp");
            m.put("username", account.getUsername());
            m.put("canRememberDevice", true);
            return m;
        }

        Map<String, Object> m = finish.execute(account, "password", request, response, true);
        if (remember) {
            var d = devices.registerAndSetCookie(account, cmd.deviceLabel(), request, response);
            m.put("trustedDeviceId", d.getId());
            m.put("trustedDevice", true);
        }
        return m;
    }

    public record PasswordLoginCommand(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password,
            Boolean rememberDevice,
            @Size(max = 128) String deviceLabel) {
        public PasswordLoginCommand(String username, String password) {
            this(username, password, null, null);
        }
    }
}
