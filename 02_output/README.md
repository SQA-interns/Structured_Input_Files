# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students fill in their own form, pick workshops, events, meals and other activities, and give the required consent. Every accepted registration is stored in PostgreSQL and as a raw JSON file, confirmed on the page and by email, and announced to the organizers by email with the JSON attached. Organizers download all registrations as an Excel workbook. Options and consents are configured in a JSON file; reCAPTCHA v2 protects the form.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API (`/api`), PostgreSQL via Flyway, JSON copies, email, Excel export | [backend/README.md](backend/README.md) |
| frontend | React single page built with Vite, served by nginx, which forwards `/api` to the backend | [frontend/README.md](frontend/README.md) |

`docker-compose.yml` runs both with PostgreSQL and the Mailpit mail catcher on `127.0.0.1`. `deploy/nginx-production.conf.example` is the reverse proxy for production (TLS, `/` → frontend, `/api` → backend).

## Quick start (local)

Prerequisites: Docker Engine 29.8.1 with Compose 5.5.1, JDK 21 (Temurin 21.0.10), Git Bash or another POSIX shell on Windows. On Windows with a long checkout path, clone with `git -c core.longpaths=true clone …`.

1. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the repository root and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters) and `ORGANIZER_EMAILS`. reCAPTCHA keys are not needed locally (test mode).
2. Build the backend jar, then start the stack from `02_output`:

   ```bash
   cd 02_output/backend && ./mvnw -B package -DskipTests && cd ..
   docker compose --env-file ../.env up -d --build
   ```

3. Open <http://127.0.0.1:8081> to register, <http://127.0.0.1:8025> for the emails (Mailpit), and download the export with the organizer login:

   ```bash
   curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" -o registrations.xlsx http://127.0.0.1:8080/api/export
   ```

4. Stop with `docker compose --env-file ../.env down` (data stays in the `pgdata` and `jsoncopies` volumes; add `-v` to delete it).

## Documentation

| Document | Content |
|---|---|
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | acceptance criteria per user story |
| [docs/02_specification.md](docs/02_specification.md) | architecture, configuration, API, storage, security design |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas, database, email, reCAPTCHA and UI contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage, mutation |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done evidence, findings, runtime demonstration |
| [docs/decisions-log.md](docs/decisions-log.md) | all decisions, including those pending review |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests |
| [logs/](logs/) | raw outputs of builds, tests and scans |
