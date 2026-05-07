# Worker Architecture Plan

## Overview

The worker is a separate Spring Boot module that polls the API for roasters due for a data refresh, scrapes them using an AI agent, and posts results back. The API owns all DB writes; the worker is stateless between jobs.

## Flow

### 1. Poll
- Worker calls `GET /internal/worker/next-job` every 15 minutes (configurable via `application.yml`)
- API finds the first roaster where:
  - No `IN_PROGRESS` agent run exists for that roaster
  - The roaster's most recent successful run is older than the configured refresh interval (or it has never been run)
- API atomically creates an `AgentRun` row with `status = IN_PROGRESS` in the same transaction
- Returns: `runId`, `websiteUrl`, `emailListUrl`, `urlHints`
- If nothing is due: returns `204 No Content` — worker sleeps until next poll

### 2. Work
- Worker runs the AI scraping agent against the returned URLs
- Worker enforces its own job timeout (configurable); if exceeded, calls back with failure

### 3. Callback
- Worker POSTs to `POST /internal/worker/runs/{runId}/complete` with structured results
- API writes `ProductObservation` / `FieldObservation` / `Promo` rows
- API updates `AgentRun` status to `SUCCESS`, `PARTIAL`, or `FAILED`
- API computes consecutive failure count from `agent_runs` history and alerts if over threshold

### 4. Timeout Enforcement (API-side)
- API has a `@Scheduled` sweep that marks any `IN_PROGRESS` run older than max runtime as `FAILED`
- Idempotent — safe to run across multiple API instances without ShedLock

## Data Design

All state lives in `agent_runs`. No new columns needed on `roasters`.

| Derived value | Query |
|---|---|
| Last successful refresh | `MAX(completed_at) WHERE roaster_id = ? AND status IN ('SUCCESS', 'PARTIAL')` |
| Currently running | `EXISTS WHERE roaster_id = ? AND status = 'IN_PROGRESS'` |
| Consecutive failures | Count backwards from most recent run until first non-failure |

Add index: `(roaster_id, started_at DESC)` on `agent_runs` to keep these fast.

Concurrency: a unique partial index on `agent_runs (roaster_id) WHERE status = 'IN_PROGRESS'` ensures two workers polling simultaneously can't both claim the same roaster.

`AgentRunStatus` needs `IN_PROGRESS` added (currently only has `SUCCESS`, `PARTIAL`, `FAILED`).

## Configuration (`application.yml`)

```yaml
batchhawk:
  worker:
    poll-interval-seconds: 15
    refresh-interval-hours: 24
    max-run-minutes: 10
    max-consecutive-failures: 3   # alert threshold
```

## New API Endpoints

Both are internal — authenticated with a shared secret header, not Keycloak.

- `GET /internal/worker/next-job` — claim next available roaster, returns job context
- `POST /internal/worker/runs/{runId}/complete` — submit results, mark run done

## What Needs to Be Built

### API module
- [ ] Add `IN_PROGRESS` to `AgentRunStatus` enum
- [ ] Add migration: unique partial index on `agent_runs (roaster_id) WHERE status = 'IN_PROGRESS'`
- [ ] Add migration: index on `agent_runs (roaster_id, started_at DESC)`
- [ ] `GET /internal/worker/next-job` endpoint + service logic
- [ ] `POST /internal/worker/runs/{runId}/complete` endpoint + service logic
- [ ] `@Scheduled` sweep to expire stale `IN_PROGRESS` runs
- [ ] Consecutive failure alerting logic
- [ ] Internal auth (shared secret filter on `/internal/**`)

### Worker module
- [ ] New Spring Boot Gradle module
- [ ] Polling loop with configurable interval
- [ ] AI agent integration (scraping)
- [ ] Job timeout enforcement
- [ ] Callback to API on completion or failure
