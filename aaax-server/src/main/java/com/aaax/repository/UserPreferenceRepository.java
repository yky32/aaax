package com.aaax.repository;

import com.aaax.entity.po.user_management.UserPreference;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepositoryImplementation<UserPreference, Long> {
    Optional<UserPreference> findByUserIdAndTypeAndKey(Long userId, String type, String key);
    List<UserPreference> findAllByUserIdAndType(Long userId, String type);

    void deleteByUserId(Long userId);
}
