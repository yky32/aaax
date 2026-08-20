package com.aaax.device;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, String> {

    List<TrustedDevice> findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(String accountId);

    Optional<TrustedDevice> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    void deleteByIdAndAccountId(String id, String accountId);
}
