package com.aaax.server.repository;

import com.aaax.server.entity.po.user_management.UserProfile;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepositoryImplementation<UserProfile, Long> {
    Optional<UserProfile> findByUserIdAndType(Long userId, String type);

    Optional<UserProfile> findByAlias(String alias);

    List<UserProfile> findByUserIdIn(Collection<Long> userIds);

    void deleteByUserId(Long userId);
}

