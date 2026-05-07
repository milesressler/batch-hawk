package com.batchhawk.data.repository;

import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.AgentRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    @Query("SELECT ar FROM AgentRun ar WHERE ar.roaster = :roaster AND ar.status != 'PENDING' ORDER BY ar.startedAt DESC LIMIT 1")
    Optional<AgentRun> findLatestCompletedOrActiveRun(@Param("roaster") Roaster roaster);

    Optional<AgentRun> findFirstByStatusOrderByCreatedAtAsc(AgentRunStatus status);

    boolean existsByRoasterAndStatusIn(Roaster roaster, Collection<AgentRunStatus> statuses);

    @Query("SELECT ar FROM AgentRun ar WHERE ar.status = :status AND ar.startedAt < :cutoff")
    List<AgentRun> findByStatusAndStartedAtBefore(
            @Param("status") AgentRunStatus status,
            @Param("cutoff") Instant cutoff);
}
