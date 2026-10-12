# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students fill in their own form, choose configurable workshops, events, meals and other activities, and get a confirmation on screen and by email. Each accepted registration is stored in PostgreSQL and as a raw JSON copy; organizers get a notification email with the JSON attached and can download all registrations as an Excel workbook. The form is protected by Google reCAPTCHA v2.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot REST API (`/api`): options, settings, registrations, organizer export; PostgreSQL, JSON copies, emails | [backend/README.md](backend/README.md) |
| frontend | React single-page form served by nginx, which also forwards `/api` to the backend | [frontend/README.md](frontend/README.md) |

The local stack (`docker-compose.yml`) runs both with PostgreSQL and the Mailpit mail catcher.

## Quick start

Prerequisites: Docker Engine with Compose, JDK 21 and Node.js 24.13.0 / npm 11.6.2 (exact versions in `../01_input/01_project/00_setup/tech-stack.md`), and a `.env` at the repository root with the keys of `01_input/01_project/00_setup/secrets.env.example` (`POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`).

```sh
cd 02_output
(cd backend && ./mvnw -B package -DskipTests)   # the backend image copies this jar
scripts/compose.sh up -d --build                # reads only the needed keys from ../.env
scripts/compose.sh ps                           # wait until every service is "healthy"
```

- Registration form: <http://127.0.0.1:8080>
- Emails (Mailpit): <http://127.0.0.1:8025>
- Export (organizer credentials from `.env`): <http://127.0.0.1:8080/api/organizer/export>

Stop with `scripts/compose.sh down` (data stays in the volumes `db-data` and `registration-copies`; `down -v` deletes it).

`scripts/compose.sh` reads only the keys it needs from `../.env`; with a `.env` whose lines are all `KEY=value` or comments, `docker compose --env-file ../.env up -d --build` works as well (D-22).

## Documentation

| Document | Content |
|---|---|
| [docs/01_acceptance-criteria.md](docs/01_acceptance-criteria.md) | acceptance criteria per user story |
| [docs/02_specification.md](docs/02_specification.md) | architecture, configuration (S11), security design, traceability |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas, database, emails, anti-automation, UI form |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, measurement exclusions, first and final runs |
| [docs/06_verification-report.md](docs/06_verification-report.md) | DoD evidence, coverage, mutation, findings, runtime demonstration |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests, pending decisions |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision D-01 … |
