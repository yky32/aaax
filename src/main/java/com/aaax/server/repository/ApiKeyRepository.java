package com.aaax.server.repository;


import com.aaax.server.entity.po.ApiKey;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepositoryImplementation<ApiKey, Long> {
}