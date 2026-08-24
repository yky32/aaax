package com.aaax.endpoint.device;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aaax.entity.po.account.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.po.device.TrustedDevice;
import com.aaax.usecase.device.TrustedDeviceUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.exception.response.AccountErrorResponse;

@RestController
@RequestMapping("/v1/devices")
@PreAuthorize("isAuthenticated()")
public class DeviceEndpoint {

    private final TrustedDeviceUseCase trustedDeviceUseCase;
    private final AccountRepository accountRepository;

    public DeviceEndpoint(TrustedDeviceUseCase trustedDeviceUseCase, AccountRepository accountRepository) {
        this.trustedDeviceUseCase = trustedDeviceUseCase;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public List<Map<String, Object>> list(Principal principal) {
        Account a = require(principal);
        return trustedDeviceUseCase.listActive(a.getId()).stream().map(this::toMap).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(
            Principal principal,
            @RequestBody(required = false) RegisterBody body,
            HttpServletRequest request,
            HttpServletResponse response) {
        Account a = require(principal);
        String label = body != null ? body.label() : null;
        TrustedDevice d = trustedDeviceUseCase.registerAndSetCookie(a, label, request, response);
        return toMap(d);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String id, Principal principal) {
        trustedDeviceUseCase.revoke(require(principal).getId(), id);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAll(Principal principal, HttpServletResponse response) {
        trustedDeviceUseCase.revokeAll(require(principal).getId());
        trustedDeviceUseCase.clearCookie(response);
    }

    private Account require(Principal principal) {
        return accountRepository.findByUsernameIgnoreCase(principal.getName())
                .orElseThrow(() -> new BizException(SystemResponse.SAU0403, "unauthorized"));
    }

    private Map<String, Object> toMap(TrustedDevice d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("label", d.getLabel());
        m.put("userAgent", d.getUserAgent());
        m.put("ip", d.getIp());
        m.put("createDt", d.getCreateDt());
        m.put("lastSeenAt", d.getLastSeenAt());
        m.put("expiresAt", d.getExpiresAt());
        return m;
    }

    public record RegisterBody(@Size(max = 128) String label) {
    }
}
