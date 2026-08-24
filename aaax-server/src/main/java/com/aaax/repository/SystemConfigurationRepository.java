package com.aaax.repository;

import com.aaax.entity.po.configuration.SystemConfiguration;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigurationRepository extends JpaRepositoryImplementation<SystemConfiguration, Long> {
    List<SystemConfiguration> findAllByTarget(String target);
    Optional<SystemConfiguration> findByTargetAndScope(String target, String scope);
    boolean existsByTargetAndScope(String target, String scope);
}