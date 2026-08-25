package com.aaax.repository;


import com.aaax.entity.po.user_verification.UserVerification;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVerificationRepository extends JpaRepositoryImplementation<UserVerification, Long> {
    Optional<UserVerification> findByUserIdAndExtIdentifier(@NonNull Long userId, @NonNull String extIdentifier);

    List<UserVerification> findByUserId(@NonNull Long userId);

    void deleteByUserId(@NonNull Long userId);
}