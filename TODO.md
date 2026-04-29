# Batch Hawk — Project TODO / Roadmap

Centralized tracker for blockers, features, and work remaining. Update as things ship.

---

## Infrastructure & DevOps

- [ ] **Terraform (`infra/`)** — Provision AWS ECS, RDS, SES, SQS, S3, IAM; activate module in `settings.gradle`
- [ ] **Secrets management** — Integrate AWS Secrets Manager or Parameter Store; remove any hardcoded secrets; wire into ECS task definitions and Spring `application.yaml` prod profile
- [ ] **Centralized logging** — Ship structured logs to a central sink (CloudWatch Logs, OpenSearch, or Loki in prod); ensure OTLP traces flow from all services (api, worker, Keycloak) to Grafana/Tempo
- [ ] **CI/CD hardening** — Add staging environment, deploy gate, and smoke tests to `buildspec.yml`

---

## Backend (api/)

### Auth & Identity
- [ ] **Typed roles in Java** — Define an enum/sealed hierarchy for roles (e.g. `ADMIN`, `MODERATOR`, `USER`, `WORKER_SERVICE`); replace magic strings in Spring Security config with type-safe constants
- [ ] **Role/permission scale definition** — Document which roles unlock which endpoints; enforce via `@PreAuthorize` annotations
- [ ] **User sync from social login** — On first OAuth2 login (Google, GitHub, etc.), sync profile fields (name, avatar, email) from the identity token into the local user record; handle re-sync on subsequent logins

### Observability & Ops
- [ ] **Audit logging** — Record security-sensitive mutations (login, role change, data delete) to a dedicated audit log table or log stream with actor, timestamp, and before/after state
- [ ] **Worker cut-off switch** — Admin-controlled flag (DB row or feature flag) that halts worker job dispatch without a redeploy; expose toggle via admin API endpoint
- [ ] **Admin tooling** — Internal-only endpoints (guarded by `ADMIN` role) for user management, job inspection, re-queue, and manual overrides

### AI / Worker
- [ ] **AI control panel** — Configuration for the AI scraping agent: prompt templates, confidence thresholds, enabled/disabled sources, cost guardrails; stored in DB and editable via admin API
- [ ] **Activate `worker` module** — Uncomment in `settings.gradle`, implement job dispatch via SQS, wire AI agent pipeline

---

## Frontend (web/)

### Auth & Routing
- [ ] **Route guards** — Protect all authenticated pages with a route guard (redirect to login if no valid session); public routes: landing, login, sign-up, product browse
- [ ] **Custom login page** — Replace default Keycloak login theme with a branded Batch Hawk login UI (Keycloak custom theme or a frontend-hosted login flow)
- [ ] **Sign-up flow** — User-facing registration page; collect required profile fields; trigger Keycloak account creation via OIDC or Keycloak Admin API
- [ ] **Google OAuth client setup** — Create Google Cloud OAuth 2.0 client ID, configure in Keycloak identity provider, test end-to-end login
- [ ] **Additional social logins** — GitHub, Apple (at minimum); configure each as a Keycloak identity provider; test token exchange and user creation
- [ ] **Post-social-login profile sync UI** — After first social login, prompt user to confirm/fill missing profile fields before continuing

### UI / UX
- [ ] **Structured layout** — Persistent nav/shell with authenticated vs. guest states; mobile-responsive
- [ ] **Error boundary & loading states** — Global error boundary, skeleton loaders for data-fetch pages
- [ ] **Frontend bug tracking integration** — Wire Sentry (or equivalent) for client-side error capture; include user context on authenticated sessions

---

## Quality & Reliability

- [ ] **Backend bug tracking** — Integrate Sentry (Java SDK) or route error events to PagerDuty/OpsGenie; attach trace IDs to reported errors
- [ ] **Integration test suite** — Expand beyond unit tests; use Testcontainers for end-to-end API tests against real Postgres + Keycloak
- [ ] **API contract enforcement** — Run `./gradlew :api:generateClientTypes` in CI; fail the build if `web/src/api-types.ts` is out of sync with the committed file

---

## Misc / Backlog

- [ ] `shared/` module — Define cross-module domain models once activated
- [ ] OpenAPI docs polish — Add descriptions and examples to all endpoints for developer UX
- [ ] Rate limiting — Protect public endpoints from abuse before going to production
- [ ] GDPR / data deletion — User account deletion flow that purges personal data
