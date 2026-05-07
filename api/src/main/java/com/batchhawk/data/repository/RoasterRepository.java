package com.batchhawk.data.repository;

import com.batchhawk.data.entity.roaster.Roaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoasterRepository extends JpaRepository<Roaster, Long>, JpaSpecificationExecutor<Roaster> {

    Optional<Roaster> findByUuid(UUID uuid);

    @Query(value = """
        SELECT r.* FROM roasters r
        LEFT JOIN (
            SELECT roaster_id, MAX(completed_at) AS last_completed
            FROM agent_runs
            WHERE completed_at IS NOT NULL
            GROUP BY roaster_id
        ) last_run ON last_run.roaster_id = r.id
        WHERE r.is_active = true
          AND r.moderation_status = 'APPROVED'
          AND NOT EXISTS (
              SELECT 1 FROM agent_runs ar
              WHERE ar.roaster_id = r.id AND ar.status IN ('PENDING', 'IN_PROGRESS')
          )
          AND (r.pending_refresh = true OR last_run.last_completed IS NULL OR last_run.last_completed < :cutoff)
        ORDER BY r.pending_refresh DESC, last_run.last_completed ASC NULLS FIRST
        """, nativeQuery = true)
    List<Roaster> findAllDueForScheduling(@Param("cutoff") Instant cutoff);
}
