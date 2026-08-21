package com.aaax.endpoint.device;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aaax.entity.po.Account;
import com.aaax.repository.AccountRepository;
import com.aaax.entity.po.TrustedDevice;
import com.aaax.service.TrustedDeviceService;

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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/devices")
@PreAuthorize("isAuthenticated()")
public class DeviceEndpoint {

    private final TrustedDeviceService devices;
    private final AccountRepository accounts;

    public DeviceEndpoint(TrustedDeviceService devices, AccountRepository accounts) {
        this.devices = devices;
        this.accounts = accounts;
    }

    @GetMapping
    public List<Map<String, Object>> list(Principal principal) {
        Account a = require(principal);
        return devices.listActive(a.getId()).stream().map(this::toMap).toList();
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
        TrustedDevice d = devices.registerAndSetCookie(a, label, request, response);
        return toMap(d);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String id, Principal principal) {
        devices.revoke(require(principal).getId(), id);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAll(Principal principal, HttpServletResponse response) {
        devices.revokeAll(require(principal).getId());
        devices.clearCookie(response);
    }

    private Account require(Principal principal) {
        return accounts.findByUsernameIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Map<String, Object> toMap(TrustedDevice d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("label", d.getLabel());
        m.put("userAgent", d.getUserAgent());
        m.put("ip", d.getIp());
        m.put("createdAt", d.getCreatedAt());
        m.put("lastSeenAt", d.getLastSeenAt());
        m.put("expiresAt", d.getExpiresAt());
        return m;
    }

    public record RegisterBody(@Size(max = 128) String label) {
    }
}
