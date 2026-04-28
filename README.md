# Batch Hawk

Specialty craft goods price & discovery app.

## Modules

| Module | Description |
|--------|-------------|
| `api/` | Spring Boot REST API — browse, search, filter, user accounts |
| `worker/` | Spring Boot scraping agent — scheduled AI-powered data collection and email monitoring |
| `web/` | React + TypeScript mobile-first frontend |
| `infra/` | Terraform — ECS, RDS, SES, SQS, S3, IAM |
| `shared/` | Shared Kotlin domain models (add when needed) |

## Local Development

### Prerequisites

- Java 25+
- Docker
- Docker Compose

### Running the API

```bash
./gradlew :api:bootRun
```

Spring Boot automatically starts the required infrastructure (PostgreSQL, Keycloak, Grafana LGTM) via Docker Compose on boot and stops them on shutdown. No separate `docker-compose up` step is needed.

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Keycloak Admin UI | http://localhost:8180 |
| Grafana | http://localhost:3000 |

The `batchhawk` Keycloak realm and `batch-hawk-web` client are imported automatically from `docker/keycloak/realms/batchhawk.json` on first start.

> **Note:** `worker/` and `web/` are not yet active.
