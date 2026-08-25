package com.aaax.server.repository;


import com.aaax.server.entity.po.user_token.UserToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepositoryImplementation<UserToken, Long> {
    @Query(value = """  
            SELECT
                ut.*
            FROM
                user_tokens ut
            WHERE
                ut.type = ?2
            AND (
                    (ut.type = 'REFRESH_TOKEN' AND ut.value ->> 'refreshToken' = ?1) OR
                    (ut.type = 'ACCESS_TOKEN' AND ut.value ->> 'accessToken' = ?1)
                );
            """,
            nativeQuery = true)
    Optional<UserToken> findByTokenValueAndTokenType(String value, String type);

    void deleteByUserId(Long userId);

    List<UserToken> findByExpireAtBefore(Instant expireAt, Pageable pageable);
}