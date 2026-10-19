# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference: external participants and students fill in a form with the configured workshops, events, meals and other activities, give the mandatory consent and pass a reCAPTCHA check. Each accepted registration is stored in PostgreSQL and as a raw JSON copy, the participant gets a confirmation email, the organizers get a notification with the JSON attached, and organizers can download all registrations as an Excel workbook.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot 4.1 REST API on Java 21 (`/api/form-config`, `/api/registrations`, `/api/registrations/export`), PostgreSQL via Flyway and JPA, SMTP, reCAPTCHA verification | [backend/README.md](backend/README.md) |
| frontend | React 19 single page built with Vite, served by nginx, which also forwards `/api` to the backend | [frontend/README.md](frontend/README.md) |

Supporting files: `docker-compose.yml` (local stack: backend, frontend, PostgreSQL, Mailpit), `config/conference.json` (the shipped conference options and consents).

## Quick start

Prerequisites: Docker Engine 29.8.1 with Compose 5.5.1, Java 21 (Temurin 21.0.10), Node 24.13.0 with npm 11.6.2. The values `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` and `ORGANIZER_EMAILS` must be set in `.env` at the repository root (copy `01_input/01_project/00_setup/secrets.env.example`).

```bash
cd 02_output/backend && ./mvnw -B package -DskipTests && cd ..
```

```bash
docker compose --env-file ../.env up -d --build --wait
```

Then open <http://127.0.0.1:8080> for the form and <http://127.0.0.1:8025> for the caught emails (Mailpit). The local stack runs reCAPTCHA in test mode: tick "I am not a robot (test mode)". The organizer export is at <http://127.0.0.1:8080/api/registrations/export> with the organizer login from `.env`.

Stop with `docker compose --env-file ../.env down` (data stays on the named volumes; add `-v` to delete it).

End-to-end tests against the running stack:

```bash
cd frontend && npm ci && npm run e2e
```

(`ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` and `ORGANIZER_EMAILS` must be exported in the shell, see the frontend README.)

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | components, backend architecture (AR-02), options mechanism (AR-04), security controls, traceability |
| [docs/02_contracts/](docs/02_contracts/) | REST API (`openapi.json`), database schema, JSON copy and configuration schemas, emails, Excel layout, UI, configuration settings |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, frozen tests, TDD baseline, coverage and mutation results |
| [docs/04_cycle-plan.md](docs/04_cycle-plan.md) | the TDD cycles |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, runtime demonstration, findings |
| [docs/release-notes.md](docs/release-notes.md) | what was delivered, limitations, manual tests |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision taken during the build |
