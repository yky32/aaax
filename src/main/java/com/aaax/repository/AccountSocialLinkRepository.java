package com.aaax.repository;

import java.util.List;
import java.util.Optional;

import com.aaax.entity.po.AccountSocialLink;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSocialLinkRepository extends JpaRepository<AccountSocialLink, String> {

    Optional<AccountSocialLink> findByProviderAndExternalId(String provider, String externalId);

    Optional<AccountSocialLink> findByAccountIdAndProvider(String accountId, String provider);

    List<AccountSocialLink> findByAccountIdOrderByCreateDtAsc(String accountId);

    long countByAccountId(String accountId);

    void deleteByAccountIdAndProvider(String accountId, String provider);
}
