CREATE TABLE agent_runs (
    id               BIGSERIAL   NOT NULL PRIMARY KEY,
    uuid             UUID        NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    roaster_id       BIGINT      NOT NULL REFERENCES roasters(id),
    started_at       TIMESTAMPTZ NOT NULL,
    completed_at     TIMESTAMPTZ,
    status           VARCHAR(20),
    fields_attempted TEXT[],
    fields_found     TEXT[],
    feedback_notes   TEXT,
    checkout_notes   TEXT,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_runs_roaster_id ON agent_runs(roaster_id);
