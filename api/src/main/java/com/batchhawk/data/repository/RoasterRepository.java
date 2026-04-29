package com.batchhawk.data.repository;

import com.batchhawk.data.entity.roaster.Roaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RoasterRepository extends JpaRepository<Roaster, UUID>, JpaSpecificationExecutor<Roaster> {

    @Query(value = """
        SELECT r.* FROM roasters r
        WHERE r.is_active = true
          AND r.moderation_status = 'APPROVED'
          AND NOT EXISTS (
              SELECT 1 FROM agent_runs ar
              WHERE ar.roaster_id = r.id AND ar.status = 'IN_PROGRESS'
          )
          AND (
              (SELECT MAX(ar.completed_at) FROM agent_runs ar
               WHERE ar.roaster_id = r.id AND ar.status IN ('SUCCESS', 'PARTIAL')) IS NULL
              OR
              (SELECT MAX(ar.completed_at) FROM agent_runs ar
               WHERE ar.roaster_id = r.id AND ar.status IN ('SUCCESS', 'PARTIAL')) < :cutoff
          )
        ORDER BY (
            SELECT MAX(ar.completed_at) FROM agent_runs ar
            WHERE ar.roaster_id = r.id AND ar.status IN ('SUCCESS', 'PARTIAL')
        ) ASC NULLS FIRST
        LIMIT 1
        """, nativeQuery = true)
    Optional<Roaster> findNextDueForRefresh(@Param("cutoff") Instant cutoff);
}
