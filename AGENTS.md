# Batch Hawk — Agent Instructions

> This is a living document. Update it as architectural decisions are made, conventions are established,
> or the plan evolves. For detailed decision records and topic-specific guidance, add files under `agents/`.

## Design Document

`batchhawk_design_review.docx` lives at the repo root and is the primary source of truth for product goals, domain model, and feature scope. It is excluded from git (`.gitignore` covers `*.docx`) — keep it local. When making architectural or feature decisions, consult it first.

## Project Overview

Batch Hawk is a specialty craft goods price-discovery and browsing app. It is a multi-module Gradle monorepo.

## Repository Structure

```
batch-hawk/
├── api/          # Spring Boot REST API (active — primary module)
├── worker/       # Spring Boot scraping/AI agent (placeholder, not yet active)
├── web/          # React + TypeScript frontend (placeholder)
├── infra/        # Terraform (ECS, RDS, SES, SQS, S3, IAM) (placeholder)
├── shared/       # Shared domain models (placeholder)
├── agents/       # Agent decision records and guidance docs
├── Dockerfile    # Builds api/ jar into Corretto 25 image
├── buildspec.yml # AWS CodeBuild pipeline (ECR push)
└── compose.yaml
```

## Tech Stack

### api/ (Spring Boot)
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.6
- **Build:** Gradle 9.x (multi-module)
- **Database:** PostgreSQL via Spring Data JPA + Flyway migrations
- **Security:** Spring Security (OAuth2 resource server — validates JWTs from Keycloak)
- **HTTP client:** Spring WebClient
- **Observability:** OpenTelemetry (`spring-boot-starter-opentelemetry`), Micrometer datasource tracing
- **Boilerplate:** Lombok (`@Data`, `@Builder`, etc.)
- **Root package:** `com.batchhawk`
- **Main class:** `com.batchhawk.service.BatchHawkDaemon`

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
- Docker image uses `public.ecr.aws/amazoncorretto/amazoncorretto:25`

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
./gradlew :api:build          # compile + test
./gradlew :api:test           # run tests only
./gradlew :api:bootRun        # run locally
./gradlew :api:bootJar        # build executable jar (outputs batch-hawk-{version}.jar)
```

## Current Plan / Roadmap

The `api` module is the active focus. High-level priorities (update as these evolve):

1. Stand up core domain models and Flyway schema
2. Implement REST endpoints for browse, search, and filter
3. Add user account management
4. ~~Wire up auth~~ ✓ Keycloak wired as OAuth2 resource server; realm auto-imported via compose
5. Activate `worker` module for AI-powered scraping and email monitoring
6. Build out `web` (React + TypeScript, mobile-first) — Vite + Mantine, builds into `api/src/main/resources/static/`
7. Provision infrastructure via `infra/` (Terraform on AWS: ECS, RDS, SES, SQS, S3)

See `agents/` for decision records and module-specific guidance as they are added.
