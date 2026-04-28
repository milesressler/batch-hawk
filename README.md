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

Starts: PostgreSQL, Keycloak, and Grafana LGTM (observability stack). The API, Worker, and Web services are run separately.

| Service | URL |
|---------|-----|
| Keycloak Admin UI | http://localhost:8180 |
| Grafana | http://localhost:3000 |

### Keycloak Setup (first time only)

Keycloak runs in `start-dev` mode with an in-memory database, so this setup must be repeated if the container is recreated.

1. Open http://localhost:8180 and log in with `admin` / `admin`
2. Create a new realm named **`batchhawk`**
3. Within the `batchhawk` realm, create a client:
   - **Client ID**: `batch-hawk-web` (or whatever the calling client is)
   - **Client authentication**: off (public client) for the web frontend
   - Set appropriate redirect URIs for your environment
4. Start the API — it fetches the OIDC discovery document from Keycloak on startup and **will fail to start** if Keycloak is not running or the `batchhawk` realm does not exist.

### Running the API

```bash
cd api
./gradlew bootRun
```

> **Note:** `worker/` and `web/` are not yet active.
