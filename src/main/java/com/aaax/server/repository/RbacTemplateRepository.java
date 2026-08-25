package com.aaax.server.repository;

import com.aaax.server.entity.po.rbac.RbacTemplate;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

@Repository
public interface RbacTemplateRepository extends JpaRepositoryImplementation<RbacTemplate, Long> {
}