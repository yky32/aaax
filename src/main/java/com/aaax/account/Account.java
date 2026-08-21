package com.aaax.account;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.aaax.core.entity.AuditableEntity;
import com.aaax.core.id.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account extends AuditableEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** Comma-separated roles without ROLE_ prefix, e.g. USER,ADMIN */
    @Column(nullable = false, length = 255)
    private String roles = "USER";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "totp_secret", length = 64)
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled = false;

    @Column(name = "google_sub", length = 128, unique = true)
    private String googleSub;

    @Column(name = "github_id", length = 64, unique = true)
    private String githubId;

    @Column(name = "saml_name_id", length = 256, unique = true)
    private String samlNameId;

    @Column(name = "phone", length = 32)
    private String phone;

    protected Account() {
    }

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

    public String getGoogleSub() {
        return googleSub;
    }

    public String getGithubId() {
        return githubId;
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

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public void setSamlNameId(String samlNameId) {
        this.samlNameId = samlNameId;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
