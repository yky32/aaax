package com.aaax.repository;

import com.aaax.entity.po.user.User;
import com.aaax.repository.projection.UserInfoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserStatisticRepository extends JpaRepositoryImplementation<User, Long> {
    @Query("""
            SELECT
                 CONCAT('u_', u.id) AS id,
                 u.username AS username,
                 u.status AS status,
                 a.attempts AS attempts,
                 a.loginType AS loginType,
                 a.lastLoginDt AS lastLoginDt
            FROM User u
            LEFT JOIN u.authentications a
            WHERE a.lastLoginDt IS NOT NULL
            ORDER BY a.lastLoginDt DESC
            """)
    Page<UserInfoProjection> userInfo(Pageable pageable);

    @Query(value = """
            SELECT
                 COUNT(*)
            FROM users u
            WHERE u.id IN (:userId)
            AND u.create_dt >= :startDt
            AND u.create_dt <= :endDt
            AND (
                 EXISTS (
                     SELECT
                         1
                     FROM user_profiles up
                     WHERE u.id = up.user_id
                     AND up.context -> 'verification' ->> 'idvStatus' IN (:statuses)
                     AND :a = 'VERIFICATION'  -- Only true when :a is 'VERIFICATION'
                 )

                 OR

                 EXISTS (
                     SELECT
                         1
                     FROM users us
                     WHERE u.id = us.id
                     AND us.status IN (:statuses)
                     AND :a = 'USER'
                 )
             )
            """, nativeQuery = true)
    long usersCount(@Param("userId") List<Long> userIds,
                    @Param("statuses") List<String> statuses,
                    @Param("startDt") Instant startDt,
                    @Param("endDt") Instant endDt,
                    @Param("a") String a
    );
}