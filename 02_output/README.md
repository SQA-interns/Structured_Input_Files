# Conference registration

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

Online registration for a conference. External participants and students fill in their own form, choose workshops, events, meals and other activities from a configurable list, give the mandatory consent and pass Google reCAPTCHA v2. Every accepted registration is stored in PostgreSQL and as a raw JSON copy, the participant gets a confirmation email, every organizer gets a notification with the JSON attached, and organizers can download all registrations as an Excel workbook.

## Components

| Component | What it is | README |
|---|---|---|
| backend | Spring Boot 4 REST API under `/api` (Java 21, PostgreSQL, Flyway, SMTP, Apache POI) | [backend/README.md](backend/README.md) |
| frontend | React 19 single page (Vite, TypeScript), served by nginx, proxies `/api` to the backend | [frontend/README.md](frontend/README.md) |

The local stack in [docker-compose.yml](docker-compose.yml) runs both with PostgreSQL 16 and the Mailpit mail catcher.

## Quick start

Prerequisites: Docker Engine with Docker Compose, JDK 21 (only to build the backend jar), Node.js 24 with npm 11 (only for frontend development and tests). Versions: [`01_input/01_project/00_setup/tech-stack.md`](../01_input/01_project/00_setup/tech-stack.md).

0. On Windows, clone with long paths enabled (some test file paths exceed 260 characters in deep folders): `git -c core.longpaths=true clone <repository>`.
1. At the repository root, copy `01_input/01_project/00_setup/secrets.env.example` to `.env` and fill `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (at least 16 characters) and `ORGANIZER_EMAILS`. `.env` is ignored by git.
2. Build the backend jar:

   ```sh
   cd 02_output/backend
   ./mvnw -B package -DskipTests
   ```

3. Start the stack from `02_output/`:

   ```sh
   docker compose --env-file ../.env up -d --build
   ```

4. Open <http://127.0.0.1:8080> for the registration form and <http://127.0.0.1:8025> for the emails caught by Mailpit. The local stack runs reCAPTCHA in test mode (a "I am not a robot (test mode)" checkbox).
5. Export as organizer: `curl -u "<ORGANIZER_USERNAME>:<ORGANIZER_PASSWORD>" -o registrations.xlsx http://127.0.0.1:8080/api/registrations/export`.
6. Stop with `docker compose --env-file ../.env down`; data stays in the volumes `pgdata` and `jsoncopies` (`down -v` deletes it).

End-to-end tests against the running stack: see [frontend/README.md](frontend/README.md#test).

## Documentation

| Document | Content |
|---|---|
| [docs/02_specification.md](docs/02_specification.md) | architecture, configuration, behaviour, security design, traceability |
| [docs/02_contracts/](docs/02_contracts/) | OpenAPI, JSON schemas, shipped options configuration, database schema, email and UI contracts |
| [docs/03_test-strategy.md](docs/03_test-strategy.md) | test levels, frozen tests, coverage and mutation results |
| [docs/06_verification-report.md](docs/06_verification-report.md) | definition-of-done evidence, runtime demonstration, findings |
| [docs/release-notes.md](docs/release-notes.md) | what is delivered, limitations, manual tests, data retention |
| [docs/decisions-log.md](docs/decisions-log.md) | every decision taken during the run |
