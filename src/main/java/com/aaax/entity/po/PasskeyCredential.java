package com.aaax.entity.po;

import com.aaax.core.entity.AuditEntityWithIsActive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(indexes = {
        @Index(name = "idx_passkey_account", columnList = "accountId"),
        @Index(name = "idx_passkey_cred", columnList = "credentialId", unique = true)
})
public class PasskeyCredential extends AuditEntityWithIsActive {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String accountId;

    @Column(nullable = false, length = 512, unique = true)
    private String credentialId;

    @Lob
    @Column(nullable = false)
    private byte[] publicKeyCose;

    @Column(length = 16)
    private byte[] aaguid;

    @Column(nullable = false)
    private long signCount;

    @Column(length = 128)
    private String label;

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
}
