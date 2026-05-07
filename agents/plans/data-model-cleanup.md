# Data Model Cleanup

**Status:** Ready  
**Touches:** `api/` entities, migrations, common DTOs

Resolves several fields that are unused, unclear, or structurally inconsistent, discovered during pre-scraper audit.

---

## Changes

### Remove `FieldObservation` entirely

The intent (per-field freshness tracking at the roaster level — shipping threshold, carrier, etc.) is valid but premature. Nothing currently writes to it, the per-field granularity is overkill for now, and per-product freshness on the `Product` entity covers the real use case. Can be revisited later.

- [x] Drop `field_observations` table (Flyway migration V00011)
- [x] Delete `FieldObservation.java` entity
- [x] Delete `FieldObservationRepository` (never existed)
- [x] Remove `ObservationSource` enum (only used by `FieldObservation`)
- [x] Remove `confidence` field (was only on FieldObservation)

---

### `AgentRun` — remove `fieldsAttempted`, keep `fieldsFound` (computed)

`fieldsAttempted` is noise — all fields are always attempted. `fieldsFound` is useful but should be computed API-side from which fields are non-null in `ProductUpdateRequest`, not sent by the worker.

- [x] Flyway migration: drop `fields_attempted` column from `agent_runs` (V00011)
- [x] Remove `fieldsAttempted` from `AgentRun.java`
- [x] Keep `fieldsFound` column + `AgentRun.fieldsFound` field
- [ ] In `WorkerJobService.completeRun()`: derive `fieldsFound` from non-null fields across all `ProductUpdateRequest` objects in the run — collect the set of `ScrapedField` values that had data

---

### `ProductObservation` — remove `valueTier`, fix `agentRunId`

`valueTier` is never set and the nightly recalculation job doesn't exist. Remove for now — if value tier is useful for the UI it can be computed at query time from current price distributions. `agentRunId` should be a proper `@ManyToOne` like every other FK in the codebase.

- [x] Flyway migration: drop `value_tier` column from `product_observations` (V00011)
- [x] Remove `valueTier` field from `ProductObservation.java`
- [x] Remove `ValueTier` enum
- [x] FK constraint `agent_run_id → agent_runs(id)` already existed in DB schema (V00005)
- [x] Change `ProductObservation.agentRunId: Long` to `ProductObservation.agentRun: AgentRun` (`@ManyToOne`, optional, lazy)
- [x] Update `WorkerJobService` to set the `AgentRun` reference on new `ProductObservation` rows

---

### `CompleteRunRequest` — token fields (from scraper plan)

Adding here since it requires a migration and DTO change.

- [x] Flyway migration: add `input_tokens INT`, `output_tokens INT` to `agent_runs` (nullable) — V00010
- [x] Add `inputTokens: Int?` and `outputTokens: Int?` to `CompleteRunRequest` (common)
- [x] `WorkerJobService.completeRun()`: write token totals to `AgentRun`

---

## Completed

- V00011 migration: dropped `field_observations`, `value_tier`, `fields_attempted`
- Deleted `FieldObservation.java`, `ObservationSource.java`, `ValueTier.java`
- Fixed `ProductObservation.agentRun` to proper `@ManyToOne`
- Token fields on `AgentRun` were already done (V00010)