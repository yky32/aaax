package com.aaax.session;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {
    List<AuthSession> findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(String accountId);

    Optional<AuthSession> findBySessionTokenAndRevokedAtIsNull(String sessionToken);
}
