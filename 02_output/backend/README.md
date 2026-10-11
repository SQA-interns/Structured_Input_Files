# Backend

> Written in: phases 0 and 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot 4.1.1 REST API for the conference registration (root package `si.konferenca.registration`, architecture in `docs/02_specification.md` section 2).

## Prerequisites

- JDK 21 (Eclipse Temurin 21.0.10). Maven is not needed: `./mvnw` downloads Maven 3.9.9.
- Docker Engine for the tests (Testcontainers starts PostgreSQL 16.15 and Mailpit v1.31.1) and for the container image.
- A POSIX shell (Git Bash on Windows) or `mvnw.cmd` on Windows cmd.

## Configuration

All settings come from environment variables (ES-01); full table in `docs/02_specification.md` section 3.

| Variable | Source | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | environment (compose sets them) | no default |
| `SPRING_DATASOURCE_PASSWORD` | `.env` → `POSTGRES_PASSWORD` | secret |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_STARTTLS`, `APP_MAIL_SMTP_AUTH`, `APP_MAIL_FROM` | environment | local: Mailpit, TLS off |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `.env` (production only) | secret |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (≥ 16 characters), `ORGANIZER_EMAILS` | `.env` | secret / recipients |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | `.env` (production) | required unless test mode |
| `APP_ENVIRONMENT` | environment | `production` (default), `local`, `test` |
| `APP_RECAPTCHA_TEST_MODE` | environment | `false` by default; refused in production |
| `APP_CONFERENCE_NAME`, `APP_CONFERENCE_CONFIG_FILE` | environment | config file required in production (schema `docs/02_contracts/conference-config.schema.json`); default is the bundled `conference-options.json` |
| `APP_JSON_COPY_DIR` | environment | `/data/json-copies` in the image |
| `APP_ORGANIZER_HTTPS_ONLY`, `APP_CORS_ALLOWED_ORIGIN` | environment | HTTPS-only may be off and CORS set only outside production |
| `APP_RATE_LIMIT_*_PER_MINUTE`, `APP_MAX_REQUEST_BYTES` | environment | defaults 10 / 120 / 10 per minute, 16384 bytes |

The backend refuses to start with an unsafe or incomplete configuration and names the setting in the error.

Changing options or consents: edit the configuration file and restart the backend; no code change.

## Commands (ES-05)

Run from `02_output/backend`.

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` |
| test (all levels, coverage report in `target/site/jacoco`) | `./mvnw -B verify` |
| check (format, lint, duplication, static analysis) | `./mvnw -B -Pcheck -DskipTests verify` (fix formatting with `./mvnw -Pcheck spotless:apply`) |
| run (local stack) | from `02_output`: `docker compose --env-file ../.env up -d --build` after the build |
| run (without Docker for the app) | set the variables above, then `./mvnw spring-boot:run` |
| dependency scan | `NVD_API_KEY=… ./mvnw -B -Pdepcheck org.owasp:dependency-check-maven:12.1.0:check` |
| mutation | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (report in `target/pit-reports`) |

Health: `GET /actuator/health/readiness` and `/actuator/health/liveness` (not under `/api`).

## Troubleshooting

| Problem | Fix |
|---|---|
| Tests fail with "Could not find a valid Docker environment" | start Docker; Testcontainers needs it |
| Start fails with "Unsafe configuration: …" | set the named variable; in production test mode, missing keys, HTTPS-only off, CORS and the bundled options file are refused |
| Start fails with "Invalid conference configuration: …" | fix the options file against `conference-config.schema.json` |
| Export answers 403 "HTTPS is required." | use HTTPS through the proxy, or set `APP_ORGANIZER_HTTPS_ONLY=false` locally |
| Requests answer 429 | rate limit per client; wait a minute |
| `docker compose build` cannot find the jar | run `./mvnw -B package -DskipTests` first |
