package com.aaax.entity.po.account;

import com.aaax.core.entity.AuditEntityWithIsActive;
import com.aaax.core.id.Ids;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Linked external login identity (qs/uaa {@code Authentication} role — 1 account : n providers).
 *
 * <p>Not denormalized onto {@link Account} (no {@code googleSub}/{@code githubId} columns).
 */
@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_social_provider_ext", columnNames = {"provider", "externalId"}),
            @UniqueConstraint(name = "uk_social_account_provider", columnNames = {"accountId", "provider"})
        })
public class AccountSocialLink extends AuditEntityWithIsActive {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    /** google | github | apple | discord | gitlab | line | slack */
    @Column(nullable = false, length = 32)
    private String provider;

    @Column(nullable = false, length = 256)
    private String externalId;

    protected AccountSocialLink() {}

    public AccountSocialLink(String accountId, String provider, String externalId) {
        this.accountId = accountId;
        this.provider = provider == null ? null : provider.toLowerCase();
        this.externalId = externalId;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = Ids.uuid();
        }
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }
}
