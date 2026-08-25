package com.aaax.server.repository;


import com.aaax.core.constant.enu.LoginType;
import com.aaax.server.entity.po.user.Authentication;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthenticationRepository extends JpaRepositoryImplementation<Authentication, Long> {

    /** Case-insensitive list by login identifier (email/username). */
    List<Authentication> findAllByIdentifierIgnoreCase(String username);

    /** Case-insensitive lookup by identifier + login type (password login paths). */
    Optional<Authentication> findByIdentifierIgnoreCaseAndLoginType(String identifier, LoginType loginType);

    /**
     * Exact match — required for social provider subjects (Apple/Google sub must stay case-sensitive).
     */
    Optional<Authentication> findByLoginTypeAndIdentifier(LoginType loginType, String identifier);

    /** Case-insensitive match for password-style link (email/mobile/username). */
    Optional<Authentication> findByLoginTypeAndIdentifierIgnoreCase(LoginType loginType, String identifier);

    List<Authentication> findByUser_Id(Long userId);
}
