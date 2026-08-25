package com.aaax.server.repository;

import com.aaax.server.entity.po.user_management.UserDevice;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepositoryImplementation<UserDevice, Long> {
    List<UserDevice> findAllByResourceIdAndResourceTypeAndUserId(String resourceId, String resourceType, Long userId);
    Optional<UserDevice> findByResourceIdAndResourceTypeAndUserId(String resourceId, String resourceType, Long userId);

    void deleteByUserId(Long userId);
}

