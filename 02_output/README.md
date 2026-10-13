# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students fill in a form, choose workshops, events, meals and other activities, and confirm the mandatory consent. Each accepted registration is stored in PostgreSQL and as a raw JSON copy, the participant gets a confirmation email and every organizer gets a notification with the JSON copy attached. Organizers download all registrations as an Excel workbook. The form is protected by Google reCAPTCHA v2.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API under `/api` (Java 21) | [backend/README.md](backend/README.md) |
| frontend | React single page built with Vite, served by nginx, which also proxies `/api` | [frontend/README.md](frontend/README.md) |

The local stack (`docker-compose.yml`) runs both components with PostgreSQL and Mailpit.

## Quick start (local stack)

Prerequisites: Docker Engine with Compose, JDK 21, Node.js 24 (versions in `01_input/01_project/00_setup/tech-stack.md`).

1. Create `.env` in the repository root (one level above this folder) from `01_input/01_project/00_setup/secrets.env.example` and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (16+ random characters) and `ORGANIZER_EMAILS`. reCAPTCHA keys are not needed locally (test mode).
2. Build the backend jar (the image copies it):
   ```bash
   cd backend && ./mvnw -B package -DskipTests && cd ..
   ```
3. Start everything:
   ```bash
   docker compose up -d --build --wait
   ```
4. Open http://127.0.0.1:8081 (form; reCAPTCHA is the test-mode checkbox), http://127.0.0.1:8025 (Mailpit: every email), and download the export with the organizer login from `.env`:
   ```bash
   curl -u "$ORGANIZER_USERNAME:$ORGANIZER_PASSWORD" -o registrations.xlsx http://127.0.0.1:8081/api/organizer/registrations.xlsx
   ```
5. Stop with `docker compose down` (data stays on the `pgdata` and `jsoncopies` volumes; `docker compose down -v` deletes it).

End-to-end tests run against this stack: `cd frontend && npm run e2e`.

On Windows, a clone in a deep folder can exceed the 260-character path limit ("Filename too long"); clone with `git -c core.longpaths=true clone <url>`.

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | architecture, API, validation, flows, security controls, configuration |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, database schema, JSON copy and configuration schemas, email and UI contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage, mutation score, quality gate |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, runtime demonstration, findings, traceability |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | decisions D-01 … D-14 |
| [logs/](logs/) | raw outputs of builds, scans and runs |
