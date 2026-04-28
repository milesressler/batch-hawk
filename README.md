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

- Java 21+
- Docker

### Infrastructure

```bash
docker-compose up
```

Starts: PostgreSQL and Grafana LGTM (observability stack). The API, Worker, and Web services are run separately.

### Running the API

```bash
cd api
./gradlew bootRun
```

> **Note:** `worker/` and `web/` are not yet active.
