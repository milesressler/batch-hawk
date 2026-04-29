-- Prevents two workers from claiming the same roaster simultaneously.
CREATE UNIQUE INDEX idx_agent_runs_roaster_in_progress
    ON agent_runs (roaster_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX idx_agent_runs_roaster_started
    ON agent_runs (roaster_id, started_at DESC);
