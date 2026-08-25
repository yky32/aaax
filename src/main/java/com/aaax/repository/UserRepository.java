package com.aaax.repository;

import com.aaax.entity.po.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepositoryImplementation<User, Long> {
    @Query(value = "SELECT u.* FROM users u WHERE u.metadata -> 'extReferenceMap' ->> :extRefKey = :extRefValue",
            nativeQuery = true)
    Optional<User> findByExtRef(@Param("extRefKey") String extRefKey, @Param("extRefValue") String extRefValue);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameIgnoreCase(String username);

    @Query(value = """
            SELECT
                COUNT(*) + 1
            FROM users
            """, nativeQuery = true)
    Long getNextAlias();

    List<User> findByUsernameNotInIgnoreCase(List<String> usernames);
}