# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (`docs/02_contracts/openapi.yaml`): form configuration, registration with reCAPTCHA verification, storage in PostgreSQL plus a JSON copy, emails, and the organizer Excel export. Architecture: ports and adapters, root package `si.konferenca.registration` (spec §2, checked by `ArchitectureTest`).

## Prerequisites

- JDK 21 (Temurin 21.0.10 pinned; the runtime image is `eclipse-temurin:21.0.10_7-jre-alpine`). Maven itself is downloaded by the wrapper (`./mvnw`, Maven 3.9.9).
- Docker for the tests (Testcontainers starts `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1`) and for the container image.

## Configuration

All settings come from environment variables (spec §3). Entries without a default are required; the backend does not start without them.

| Variable | Default | Source |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME` | none | environment (compose sets them locally) |
| `POSTGRES_PASSWORD` | none | secret, `.env` |
| `SMTP_HOST`, `SMTP_PORT` | none | environment (Mailpit locally) |
| `SMTP_STARTTLS` | `true` | environment (`false` for Mailpit) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty | secret, production only |
| `MAIL_FROM` | `registration@konferenca.local` | environment |
| `ORGANIZER_EMAILS` | none | secret, `.env` (comma separated) |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none | secret, `.env`; password ≥ 16 characters, kept only as a BCrypt hash |
| `ORGANIZER_HTTPS_ONLY` | `true` | environment; `false` only on local plain HTTP |
| `CONFERENCE_NAME` | `Conference` | environment |
| `CONFERENCE_CONFIG` | none | location of the options file, e.g. `file:/config/conference.json`; shipped file: [config/conference.json](config/conference.json), schema `docs/02_contracts/conference-config.schema.json` |
| `JSON_COPY_DIR` | `/data/json-copies` | environment (persistent volume) |
| `RECAPTCHA_TEST_MODE` | `false` | environment; `true` only for test and local |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty | secret, production only |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | environment |
| `CORS_ALLOWED_ORIGINS` | empty | local Vite dev server only |
| `RATE_LIMIT_FORM_CONFIG_PER_MINUTE`, `RATE_LIMIT_REGISTRATIONS_PER_MINUTE`, `RATE_LIMIT_EXPORT_PER_MINUTE` | 60, 10, 10 | environment |
| `REQUEST_MAX_BODY_BYTES` | 16384 | environment |
| `SPRING_PROFILES_ACTIVE` | none | `prod` in production: start-up fails with test mode on, empty reCAPTCHA keys or HTTPS-only off |

Changing the offered options: edit the options file and restart; an invalid file stops start-up with a message naming the problem.

## Build

```sh
./mvnw -B package -DskipTests        # target/registration-backend-0.1.0.jar
docker build -t registration-backend .   # after the jar exists
```

## Run

Locally the backend runs in the compose stack (root README). To run the jar directly, export the required variables above, then `java -jar target/registration-backend-0.1.0.jar`. Health: `/actuator/health/liveness`, `/actuator/health/readiness`.

## Test

```sh
./mvnw -B verify                     # all 203 tests: unit, integration, architecture, acceptance (Docker needed)
./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check   # check: format, lint, static analysis
./mvnw -B spotless:apply             # format
./mvnw -B jacoco:report              # coverage report after a test run (target/site/jacoco)
./mvnw -B org.pitest:pitest-maven:mutationCoverage   # mutation testing (about 15 minutes)
```

Dependency scan (needs `NVD_API_KEY` exported from `.env`): `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Could not resolve placeholder 'DATABASE_URL'` (or another variable) at start-up | a required variable is missing; set it |
| `ORGANIZER_PASSWORD must have at least 16 characters` | use a longer organizer password in `.env` |
| `Option '…' has unknown category …` or `Duplicate option id …` | fix the options file named in `CONFERENCE_CONFIG` |
| tests fail with `Could not find a valid Docker environment` | start Docker; the acceptance tests use Testcontainers |
| registration answers 500 "could not be stored" | `JSON_COPY_DIR` is not writable; nothing was stored |
| emails missing but registration accepted | SMTP unreachable; the log shows `Email … for registration <id> failed` |
| export answers 403 "requires HTTPS" | call it over HTTPS, or set `ORGANIZER_HTTPS_ONLY=false` on local plain HTTP only |
