package com.aaax.entity.po.account;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.id.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

/**
 * Core account (qs/uaa {@code User} shape — identities live in {@link AccountSocialLink}, not denormalized columns).
 */
@Entity
public class Account extends AuditEntityWithIsActive {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    /** Comma-separated roles without ROLE_ prefix, e.g. USER,ADMIN */
    @Column(nullable = false, length = 255)
    private String roles = "USER";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 64)
    private String totpSecret;

    @Column(nullable = false)
    private boolean totpEnabled = false;

    /** SAML SP name-id (enterprise federation — not social OAuth). */
    @Column(length = 256, unique = true)
    private String samlNameId;

    @Column(length = 32)
    private String phone;

    protected Account() {}

    public Account(String username, String email, String passwordHash) {
        this(username, email, passwordHash, "USER");
    }

    public Account(String username, String email, String passwordHash, String roles) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles == null || roles.isBlank() ? "USER" : roles;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = Ids.uuid();
        }
        if (roles == null || roles.isBlank()) {
            roles = "USER";
        }
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public Set<String> roleSet() {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public String getSamlNameId() {
        return samlNameId;
    }

    public String getPhone() {
        return phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public void setSamlNameId(String samlNameId) {
        this.samlNameId = samlNameId;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
