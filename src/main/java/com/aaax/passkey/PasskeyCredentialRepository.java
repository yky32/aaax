package com.aaax.passkey;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, String> {
    List<PasskeyCredential> findByAccountIdOrderByCreatedAtDesc(String accountId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    void deleteByIdAndAccountId(String id, String accountId);
}
