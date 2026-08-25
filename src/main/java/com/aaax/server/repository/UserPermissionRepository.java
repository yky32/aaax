package com.aaax.server.repository;

import com.aaax.server.entity.po.user_management.UserPermission;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepositoryImplementation<UserPermission, Long> {
    Optional<UserPermission> findByUserId(@NonNull Long userId);

    void deleteByUserId(Long userId);
}