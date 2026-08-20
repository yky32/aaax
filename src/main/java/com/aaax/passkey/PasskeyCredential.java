package com.aaax.passkey;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "aaax_passkey", indexes = {
        @Index(name = "idx_passkey_account", columnList = "account_id"),
        @Index(name = "idx_passkey_cred", columnList = "credential_id", unique = true)
})
public class PasskeyCredential {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "credential_id", nullable = false, length = 512, unique = true)
    private String credentialId;

    @Lob
    @Column(name = "public_key_cose", nullable = false)
    private byte[] publicKeyCose;

    @Column(name = "aaguid", length = 16)
    private byte[] aaguid;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "label", length = 128)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void pre() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getPublicKeyCose() {
        return publicKeyCose;
    }

    public void setPublicKeyCose(byte[] publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public byte[] getAaguid() {
        return aaguid;
    }

    public void setAaguid(byte[] aaguid) {
        this.aaguid = aaguid;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
