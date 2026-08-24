package com.aaax.repository;

import com.aaax.entity.po.rbac.RbacTemplate;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

@Repository
public interface RbacTemplateRepository extends JpaRepositoryImplementation<RbacTemplate, Long> {
}