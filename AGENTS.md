# Batch Hawk — Agent Instructions

> This is a living document. Update it as architectural decisions are made, conventions are established,
> or the plan evolves. For detailed decision records and topic-specific guidance, add files under `agents/`.

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
└── docker-compose.yml
```

## Tech Stack

### api/ (Spring Boot)
- **Language:** Java 25
- **Framework:** Spring Boot 4.0.6
- **Build:** Gradle 9.x (multi-module)
- **Database:** PostgreSQL via Spring Data JPA + Flyway migrations
- **Security:** Spring Security
- **HTTP client:** Spring WebClient
- **Observability:** OpenTelemetry (`spring-boot-starter-opentelemetry`), Micrometer datasource tracing
- **Boilerplate:** Lombok (`@Data`, `@Builder`, etc.)
- **Root package:** `com.batchhawk`
- **Main class:** `com.batchhawk.service.BatchHawkDaemon`

### Local Dev
```bash
docker-compose up   # starts PostgreSQL + Grafana LGTM observability stack
./gradlew :api:bootRun
```

Docker compose provides:
- PostgreSQL on port 5432 (db/user/pass: `batchhawk`)
- Grafana LGTM (Loki, Grafana, Tempo, Mimir) on port 3000; OTLP on 4317/4318

## Development Conventions

- **One module at a time:** `worker`, `web`, `infra`, `shared` are stubs. Only `api` is included in `settings.gradle`. Uncomment modules there when activating them.
- **Flyway for all DB changes:** Never modify the schema directly; add migration files under `api/src/main/resources/db/migration/`.
- **Lombok everywhere:** Use `@Data`, `@Builder`, `@RequiredArgsConstructor`, etc. Do not write boilerplate getters/setters manually.
- **No manual schema creation:** Let Flyway own the schema; do not use `spring.jpa.hibernate.ddl-auto=create`.
- **OpenTelemetry-first observability:** Use structured logging and traces. The LGTM stack aggregates them locally.
- **Security:** All endpoints require authentication by default unless explicitly configured otherwise.
- **Tests:** JUnit 5 via `useJUnitPlatform()`. Run with `./gradlew :api:test`.

## Key Gradle Commands

```bash
./gradlew :api:build          # compile + test
./gradlew :api:test           # run tests only
./gradlew :api:bootRun        # run locally
./gradlew :api:bootJar        # build executable jar
```

## Current Plan / Roadmap

The `api` module is the active focus. High-level priorities (update as these evolve):

1. Stand up core domain models and Flyway schema
2. Implement REST endpoints for browse, search, and filter
3. Add user account management
4. Wire up Spring Security (authentication + authorization)
5. Activate `worker` module for AI-powered scraping and email monitoring
6. Build out `web` (React + TypeScript, mobile-first)
7. Provision infrastructure via `infra/` (Terraform on AWS: ECS, RDS, SES, SQS, S3)

See `agents/` for decision records and module-specific guidance as they are added.