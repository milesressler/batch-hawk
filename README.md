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

```bash
docker-compose up
```

Starts: PostgreSQL, API, Worker, Web (React dev server).

## Design

See [docs/batchhawk design review.docx](docs/batchhawk%20design%20review.docx) for the full design review.
