# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. Participants register as **external participants** or **students**, select workshops, events, meals and other activities, give the mandatory consent and pass reCAPTCHA. Every accepted registration is stored in PostgreSQL and as a raw JSON copy, confirmed to the participant by email and sent to the organizers with the JSON copy attached. Organizers download all registrations as an Excel workbook.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API under `/api` (Java 21): validation, storage, JSON copies, emails, export | [backend/README.md](backend/README.md) |
| frontend | React single-page form (Vite), served by nginx, which also forwards `/api` to the backend | [frontend/README.md](frontend/README.md) |

The local stack is defined in [docker-compose.yml](docker-compose.yml): backend, frontend, PostgreSQL 16 and the Mailpit mail catcher.

## Quick start

Prerequisites: Docker Engine 29.8.1 with Compose 5.5.1, JDK 21, Node.js 24.13.0 (npm 11.6.2). Run the commands from the repository root unless stated otherwise.

1. Create `.env` in the repository root from `01_input/01_project/00_setup/secrets.env.example` and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 random characters) and `ORGANIZER_EMAILS`. The reCAPTCHA and SMTP keys stay empty locally.
2. Build the backend jar (the backend image copies it):

   ```bash
   cd 02_output/backend && ./mvnw -B -DskipTests package
   ```

3. Start the stack from `02_output/`:

   ```bash
   docker compose --env-file ../.env up -d --build
   ```

4. Open <http://127.0.0.1:8080> for the form and <http://127.0.0.1:8025> for the emails in Mailpit. Locally the reCAPTCHA runs in test mode: tick "I am not a robot (test mode)".
5. Download the export: `curl -u "<organizer>:<password>" -o registrations.xlsx http://127.0.0.1:8080/api/export`.
6. Stop with `docker compose --env-file ../.env down` (data stays on the named volumes; `down -v` deletes it).

End-to-end tests against the running stack: `cd 02_output/frontend && npm ci && npx playwright install chromium && npm run test:e2e`.

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | specification, architecture, configuration, traceability |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas, database, email, reCAPTCHA and UI contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, runs, coverage and mutation figures |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition of done, security evidence, findings, traceability |
| [docs/release-notes.md](docs/release-notes.md) | delivered scope, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | decisions D-01 … D-16 |
| [docs/00_preflight-report.md](docs/00_preflight-report.md) | tool, service and dependency checks |
