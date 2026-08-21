package com.aaax.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aaax.entity.po.TrustedDevice;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, String> {

    List<TrustedDevice> findByAccountIdAndRevokedAtIsNullOrderByLastSeenAtDesc(String accountId);

    Optional<TrustedDevice> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    void deleteByIdAndAccountId(String id, String accountId);
}
