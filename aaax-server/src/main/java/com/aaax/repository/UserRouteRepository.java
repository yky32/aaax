package com.aaax.repository;

import com.aaax.entity.po.UserRoute;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRouteRepository extends JpaRepositoryImplementation<UserRoute, Long> {
    List<UserRoute> findAllByUserId(Long userId);
    List<UserRoute> findAllByTenantRoleRouteIdIn(List<Long> trrIds);
    Optional<UserRoute> findByTenantRoleRouteIdAndUserId(@NonNull Long tenantRoleRouteId, @NonNull Long userId);

    Optional<UserRoute> findByUserIdAndTenantRoleRouteId(@NonNull Long userId, @NonNull Long tenantRoleRouteId);

    void deleteByUserId(Long userId);
}