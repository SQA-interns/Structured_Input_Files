# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students register through a web form, choose configurable workshops, events, meals and other activities, and give the required consents. Every accepted registration is stored in PostgreSQL and as a raw JSON copy, confirmed in the application and by email, and announced to the organizers by email with the JSON attached. Organizers download all registrations as an Excel workbook. The form is protected by Google reCAPTCHA v2, rate limits and server-side validation.

## Components

| Component | Folder | What it is |
|---|---|---|
| backend | [`backend/`](backend/README.md) | Spring Boot REST API under `/api`: form configuration, registration, organizer export; PostgreSQL (Flyway), JSON copies, SMTP, reCAPTCHA verification |
| frontend | [`frontend/`](frontend/README.md) | React single-page registration form, served by unprivileged nginx, which also forwards `/api` to the backend |

Supporting files: [`docker-compose.yml`](docker-compose.yml) (local stack: backend, frontend, PostgreSQL, Mailpit) and [`config/conference-options.json`](config/conference-options.json) (the options and consents the local stack offers).

## Quick start

Prerequisites: Eclipse Temurin JDK 21.0.10, Docker Engine with Docker Compose 5.5.1 (versions in `01_input/01_project/00_setup/tech-stack.md`), and a `.env` file in the repository root created from `01_input/01_project/00_setup/secrets.env.example` with at least `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (16 or more characters) and `ORGANIZER_EMAILS`.

```bash
cd 02_output/backend
./mvnw -B -DskipTests package
cd ..
docker compose --env-file ../.env up -d --build --wait
```

Then open:

| URL | What |
|---|---|
| http://127.0.0.1:8081 | registration form (reCAPTCHA test mode: tick "I am not a robot (test mode)") |
| http://127.0.0.1:8025 | Mailpit: the confirmation and organizer emails |
| http://127.0.0.1:8081/api/export/registrations.xlsx | Excel export (organizer credentials from `.env`) |
| http://127.0.0.1:8080/actuator/health/readiness | backend readiness |

Stop with `docker compose --env-file ../.env down`; data stays in the volumes `pgdata` and `jsoncopies` (add `-v` to delete it).

To change the offered options, edit `config/conference-options.json` (schema: `docs/02_contracts/conference-options.schema.json`) and restart the backend: `docker compose --env-file ../.env restart backend`.

## Documentation

| Document | Content |
|---|---|
| [`docs/01_acceptance-criteria.md`](docs/01_acceptance-criteria.md) | acceptance criteria per user story |
| [`docs/02_specification.md`](docs/02_specification.md) | architecture, configuration, API, security, traceability |
| [`docs/02_contracts/`](docs/02_contracts/) | OpenAPI, JSON schemas, database schema, emails, reCAPTCHA, UI |
| [`docs/03_test-strategy.md`](docs/03_test-strategy.md) | test levels, runs, coverage and mutation figures |
| [`docs/06_verification-report.md`](docs/06_verification-report.md) | Definition of Done evidence, security review, findings |
| [`docs/release-notes.md`](docs/release-notes.md) | what is delivered, limitations, manual tests, pending decisions |
| [`docs/decisions-log.md`](docs/decisions-log.md) | every decision made during the build |
| [`docs/00_preflight-report.md`](docs/00_preflight-report.md) | environment and dependency checks |
