# Batch Hawk — Agent Instructions

> This is a living document. Update it as architectural decisions are made, conventions are established,
> or the plan evolves. For detailed decision records and topic-specific guidance, add files under `agents/`.

## Design Document

`private/design-doc.md` is the primary source of truth for product goals, domain model, and feature scope. It is excluded from git (`.gitignore` covers `private/`) — keep it local. When making architectural or feature decisions, consult it first.

When a decision is made that resolves an open question, changes the data model, adds a feature, or alters the architecture, update `private/design-doc.md` to reflect it — move resolved items to the RESOLVED section in §4, update the relevant schema or architecture section, and bump the version number in the header.

## Project Overview

Batch Hawk is a specialty craft goods price-discovery and browsing app. It is a multi-module Gradle monorepo.

## Repository Structure

```
batch-hawk/
├── api/          # Spring Boot REST API (active)
├── worker/       # Spring Boot scraping/AI agent (active)
├── common/       # Shared DTOs between api/ and worker/ (active)
├── web/          # React + TypeScript frontend (active)
├── infra/        # Terraform (ECS, RDS, SES, SQS, S3, IAM) (placeholder)
├── agents/       # Agent decision records and guidance docs
├── Dockerfile    # Builds api/ jar into Corretto 21 image
├── buildspec.yml # AWS CodeBuild pipeline (ECR push)
└── compose.yaml
```

## Tech Stack

### api/ (Spring Boot)
- **Language:** Java 21
- **Framework:** Spring Boot 4.0.6
- **Build:** Gradle 9.x (multi-module)
- **Database:** PostgreSQL via Spring Data JPA + Flyway migrations
- **Security:** Spring Security (OAuth2 resource server — validates JWTs from Keycloak)
- **HTTP client:** Spring WebClient
- **Observability:** OpenTelemetry (`spring-boot-starter-opentelemetry`), Micrometer datasource tracing
- **Boilerplate:** Lombok (`@Data`, `@Builder`, etc.)
- **Root package:** `com.batchhawk`
- **Main class:** `com.batchhawk.BatchHawkApplication`

### Authentication — Keycloak (DECIDED, not yet implemented)
- **Decision:** Use Keycloak as a self-hosted OIDC/OAuth2 authorization server
- **Why:** Social login (Google, GitHub, Apple, etc.) out of the box, full user data ownership (users + bcrypt hashes in our own Postgres), no per-MAU vendor costs, no lock-in
- **Rejected:** Auth0 (lock-in, cost at scale), AWS Cognito (no password hash export = hard lock-in), DIY username/password (would need to rebuild social login plumbing from scratch)
- **Deployment:** Keycloak runs as a separate ECS service backed by Postgres (same RDS instance, separate schema or DB)
- **api/ role:** Pure OAuth2 resource server — validates JWTs issued by Keycloak via `spring-boot-starter-oauth2-resource-server`
- **Status:** Implemented. Keycloak in `compose.yaml`, `spring-boot-starter-oauth2-resource-server` in `api/build.gradle`, `issuer-uri` configured in `application.yaml`.

### Local Dev
```bash
./gradlew :api:bootRun
```

Spring Boot (`spring-boot-docker-compose`) auto-starts `compose.yaml` on boot and stops it on shutdown — no separate `docker-compose up` needed. Services provided:
- PostgreSQL on port 5432 (db/user/pass: `batchhawk`) — datasource auto-configured
- Keycloak on port 8180 — `batchhawk` realm and `batch-hawk-web` client auto-imported from `docker/keycloak/realms/batchhawk.json`
- Grafana LGTM (Loki, Grafana, Tempo, Mimir) on port 3000; OTLP on 4317/4318

### CI/CD
- `buildspec.yml` — AWS CodeBuild. Requires `REPOSITORY_URI` env var set in the CodeBuild project pointing to the ECR repo.
- Builds `./gradlew :api:build :api:bootJar`, produces `api/build/libs/batch-hawk-{version}.jar`
- Docker image uses `public.ecr.aws/amazoncorretto/amazoncorretto:21`

### worker/ (Spring Boot — Kotlin)
- **Language:** Kotlin (JVM 21)
- **Framework:** Spring Boot, scheduled polling via `@Scheduled`
- **Build:** Gradle, included in multi-module build
- **Browser automation:** Microsoft Playwright (`com.microsoft.playwright:playwright:1.51.0`) — singleton `Playwright` + `Browser` beans, one `BrowserContext` per scraping session
- **LLM:** Anthropic Java SDK (`com.anthropic:anthropic-java:2.17.0`) — Claude Haiku for scraping sessions
- **Root package:** `com.batchhawk.worker`
- **Key commands:**
  ```bash
  ./gradlew :worker:bootRun    # run locally
  ./gradlew :worker:build      # compile + test
  ```

#### Worker Architecture

```
WorkerPoller (polls API every N seconds)
  → claims NextJobResponse (runId, websiteUrl, integrationType, urlHints)
  → dispatches to RoasterScraper by integrationType name
      SHOPIFY  → ShopifyProductScraper
      CUSTOM   → PlaywrightAgentScraper (also used as fallback)
  → calls completeRun with CompleteRunRequest
```

#### Playwright Scraper Components

| Class | Role |
|---|---|
| `PlaywrightConfig` | `@Configuration` — singleton `Playwright` + `Browser` beans, `@PreDestroy` shutdown |
| `BrowserManager` | Single-permit `Semaphore` wrapping `Browser`; `withContext { BrowserContext -> T }` creates/closes context per session |
| `BrowserTools` | Executes all 7 scraping tools against a `Page`; owns preprocessing and `TerminalResult` |
| `ScrapingToolDefinitions` | Defines tool schemas as `List<Tool>` |
| `ScraperAgentSession` | Drives the Claude Haiku agentic loop; accumulates token counts; builds `CompleteRunRequest` |
| `PlaywrightAgentScraper` | `@Component("CUSTOM")` entry point; wires `BrowserTools` + `ScraperAgentSession` inside a `BrowserManager.withContext` block |

**Tools:** `navigate`, `get_page_content`, `click`, `scroll_to_bottom`, `extract_links`, `return_products`, `report_failure`

**Page preprocessing:** strips scripts/styles/nav/footer/cookie banners → plain text → truncated to `pageTextMaxChars` (config)

#### Site Hints (Forward Context)

Each roaster has a `url_hints` JSONB column (`producers` table). After a successful scrape, discovered site structure is written back and returned on the next job:

```json
{
  "productListingUrls": ["/product-category/coffee/"],
  "paginationType": "paged_param",
  "paginationParam": "paged",
  "requiresDetailPage": true,
  "platformType": "woocommerce",
  "failedStrategies": ["?page=N"]
}
```

The agent prompt includes these hints on repeat runs to skip discovery and go straight to known pages.

#### Scraping Config (`batchhawk.worker.scraping`)

| Key | Default | Purpose |
|---|---|---|
| `max-turns` | 12 | Hard cap on agentic loop turns |
| `navigation-timeout-ms` | 15000 | Playwright navigate + click timeout |
| `page-text-max-chars` | 12000 | Preprocessed page text truncation |
| `max-links` | 50 | Max results from `extract_links` |

#### Token Tracking

`ScraperAgentSession` accumulates `inputTokens` + `outputTokens` across all turns and includes them in `CompleteRunRequest`. The API writes these to `agent_runs.input_tokens` / `agent_runs.output_tokens`.

## Research Conventions

- **Never inspect dependency JARs** to learn SDK APIs (e.g., `jar tf`, `javap`). Use web search, official documentation, or read existing usages in the codebase instead.

## Kotlin Conventions

- **No wildcard imports:** Always use explicit imports (`import com.foo.Bar`), never `import com.foo.*`.

## Development Conventions

- **One module at a time:** `worker`, `web`, `infra`, `shared` are stubs. Only `api` is included in `settings.gradle`. Uncomment modules there when activating them.
- **Flyway for all DB changes:** Never modify the schema directly; add migration files under `api/src/main/resources/db/migration/`. Use naming convention `V00001__description.sql` (5-digit padded).
- **Lombok everywhere:** Use `@Data`, `@Builder`, `@RequiredArgsConstructor`, etc. Do not write boilerplate getters/setters manually.
- **No manual schema creation:** Let Flyway own the schema; do not use `spring.jpa.hibernate.ddl-auto=create`.
- **OpenTelemetry-first observability:** Use structured logging and traces. The LGTM stack aggregates them locally.
- **Security:** All endpoints require authentication by default unless explicitly configured otherwise.
- **Tests:** JUnit 5 via `useJUnitPlatform()`. Run with `./gradlew :api:test`.
- **application.yaml:** Multi-profile structure (default / dev / prod) in a single file using `---` separators. Dev profile datasource is auto-configured by `spring-boot-docker-compose` (no hardcoded URL). Prod reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from env vars.

## Key Gradle Commands

```bash
./gradlew :api:build                    # compile + test
./gradlew :api:test                     # run tests only
./gradlew :api:bootRun                  # run locally
./gradlew :api:bootJar                  # build executable jar (outputs batch-hawk-{version}.jar)
./gradlew :api:generateClientTypes      # regenerate frontend TypeScript types from OpenAPI spec
```

## API Contract Sync

The frontend TypeScript types (`web/src/api-types.ts`) are generated from the backend's OpenAPI spec. After any API change (new endpoint, changed response DTO), run:

```bash
./gradlew :api:generateClientTypes
```

This boots `TestBatchHawkApplication` (testcontainers Postgres + no-op `JwtDecoder` via `LocalDevSecurityConfig`), captures `/v3/api-docs`, writes `web/openapi.json`, runs `npm run generate:api` to produce `web/src/api-types.ts`, then shuts down.

Always commit the updated `api-types.ts` alongside the API change. `openapi.json` is gitignored (build artifact).

- Required response fields are annotated `@Schema(requiredMode = REQUIRED)` on the Java record — this drives non-optional types in the generated TypeScript.
- Swagger UI: http://localhost:8080/swagger-ui.html (no auth required)
- OpenAPI spec: http://localhost:8080/v3/api-docs

## Frontend (web/)

```bash
cd web && npm install && npm run dev    # dev server at http://localhost:5173
```

No `.env.local` needed for local dev — `vite.config.ts` proxies `/api/*` to `http://localhost:8080` automatically. In production, set `VITE_API_BASE_URL` to the actual API origin.

Key files:
- `web/src/services/client.ts` — openapi-fetch typed client; call `setTokenGetter()` once Keycloak auth is wired in the frontend
- `web/src/services/roastersApi.ts`, `productsApi.ts` — per-resource service functions
- TanStack Query (`@tanstack/react-query`) is the data-fetching layer; `QueryClientProvider` is in `main.tsx`

## Current Plan / Roadmap

The `api` module is the active focus. High-level priorities (update as these evolve):

1. Stand up core domain models and Flyway schema
2. Implement REST endpoints for browse, search, and filter
3. Add user account management
4. ~~Wire up auth~~ ✓ Keycloak wired as OAuth2 resource server; realm auto-imported via compose
5. Activate `worker` module for AI-powered scraping and email monitoring
6. ~~Build out `web`~~ ✓ React + TypeScript (Vite), TanStack Query, openapi-fetch typed client wired against generated OpenAPI types
7. Provision infrastructure via `infra/` (Terraform on AWS: ECS, RDS, SES, SQS, S3)

See `agents/` for decision records and module-specific guidance as they are added.

## Active Plans

`agents/plans/` acts as a living task board — one file per in-progress effort. Each plan tracks goal, architecture decisions, task checklist (with completion status), and open questions.

**When working on a feature or module:**
- Check `agents/plans/` for an existing plan before starting
- Mark tasks `[x]` as they are completed
- Move finished items to the **Completed** section
- Add new open questions or follow-up work as they surface
- Update architecture notes if decisions change during implementation

Current plans:
- [`agents/plans/playwright-two-stage-scraper.md`](agents/plans/playwright-two-stage-scraper.md) — Refactor Playwright scraper into discovery + per-product detail stages
- [`agents/plans/data-model-cleanup.md`](agents/plans/data-model-cleanup.md) — Remove FieldObservation, clean up AgentRun fields, fix ProductObservation FK
- [`agents/plans/product-identity.md`](agents/plans/product-identity.md) — Product URL, deduplication, lastRefreshedAt, inactive product handling
- [`agents/plans/scraping-strategy.md`](agents/plans/scraping-strategy.md) — IntegrationType vs. scraping mechanism, Playwright as fallback not a type
