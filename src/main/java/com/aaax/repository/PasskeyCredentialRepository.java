package com.aaax.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aaax.entity.po.PasskeyCredential;

public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, String> {
    List<PasskeyCredential> findByAccountIdOrderByCreatedAtDesc(String accountId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    void deleteByIdAndAccountId(String id, String accountId);
}
