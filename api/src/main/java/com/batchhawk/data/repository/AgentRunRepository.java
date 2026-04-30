package com.batchhawk.data.repository;

import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.enums.AgentRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {

    @Query("SELECT ar FROM AgentRun ar WHERE ar.status = :status AND ar.startedAt < :cutoff")
    List<AgentRun> findByStatusAndStartedAtBefore(
            @Param("status") AgentRunStatus status,
            @Param("cutoff") Instant cutoff);
}
