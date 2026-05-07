ALTER TABLE agent_runs
    ADD COLUMN input_tokens  BIGINT,
    ADD COLUMN output_tokens BIGINT;
