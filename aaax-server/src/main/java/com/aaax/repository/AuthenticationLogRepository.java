package com.aaax.repository;


import com.aaax.entity.po.log.AuthenticationLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface AuthenticationLogRepository extends JpaRepositoryImplementation<AuthenticationLog, Long> {

    @Query(value = """
            SELECT COUNT(DISTINCT action_by)
            FROM authentication_log
            WHERE create_dt >= :fromDt
              AND create_dt < :toDt
              AND event NOT IN (:excludedEvents)
              AND action_by IS NOT NULL
            """, nativeQuery = true)
    long countDistinctUsersBetween(
            @Param("fromDt") Instant fromDt,
            @Param("toDt") Instant toDt,
            @Param("excludedEvents") Collection<String> excludedEvents
    );

    @Query(value = """
            SELECT CAST((create_dt AT TIME ZONE 'UTC') AT TIME ZONE :timezone AS date) AS day,
                   COUNT(DISTINCT action_by) AS unique_users
            FROM authentication_log
            WHERE create_dt >= :fromDt
              AND create_dt < :toDt
              AND event NOT IN (:excludedEvents)
              AND action_by IS NOT NULL
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> countDistinctUsersGroupedByDay(
            @Param("fromDt") Instant fromDt,
            @Param("toDt") Instant toDt,
            @Param("timezone") String timezone,
            @Param("excludedEvents") Collection<String> excludedEvents
    );
}
