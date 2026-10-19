# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API for the conference registration (contract: `../docs/02_contracts/openapi.json`). Packages under `si.konferenca.registration`: `domain` (rules), `application` (use cases and ports), `web` (controllers, security, filters), `infrastructure` (JPA, JSON copy, mail, reCAPTCHA, Excel, configuration).

## Prerequisites

- Java 21 (Temurin 21.0.10); Maven 3.9.9 is downloaded by the wrapper `./mvnw` (3.3.2).
- Docker Engine 29.8.1 for the tests (Testcontainers starts PostgreSQL 16.15 and Mailpit v1.31.1) and for the container image.

## Configuration

Every setting is an environment variable; the full list with defaults is in `../docs/02_contracts/configuration.md`.

| Setting | Source | Note |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | environment (compose sets them) | required |
| `POSTGRES_PASSWORD` (or `SPRING_DATASOURCE_PASSWORD`) | `.env` (secret) | required |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS`, `MAIL_FROM` | environment | `MAIL_TLS` defaults to `true` (STARTTLS required) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `.env` (secret, production only) | |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | `.env` (secret) | required; the password is kept only as a BCrypt hash |
| `ORGANIZER_HTTPS_ONLY` | environment | default `true`; `false` only on local plain HTTP |
| `CONFERENCE_CONFIG_FILE` | environment | required; JSON file per `conference-config.schema.json`; an invalid file stops the start |
| `JSON_COPY_DIR` | environment | default `/data/registrations` (persistent volume) |
| `APP_ENVIRONMENT` | environment | `production` (default), `local` or `test` |
| `RECAPTCHA_TEST_MODE`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_VERIFY_URL` | environment / `.env` (secret key) | production refuses to start with test mode on or empty keys |
| `RATE_LIMIT_*_PER_MINUTE`, `MAX_REQUEST_BYTES` | environment | defaults 60 / 10 / 10 per minute, 16384 bytes |

## Build

```bash
./mvnw -B package -DskipTests
```

The jar is `target/registration-backend-1.0.0.jar`; the image is built from `Dockerfile` (runs as a non-root user) by `docker compose build backend`.

## Run

With the whole local stack (recommended): see `../README.md`. Alone, with PostgreSQL and an SMTP server reachable and the variables above exported:

```bash
./mvnw spring-boot:run
```

Health: `/actuator/health/liveness` and `/actuator/health/readiness` on port 8080.

## Test

```bash
./mvnw -B test
```

Runs unit, integration, architecture (ArchUnit) and the frozen acceptance tests (`src/test/java/.../acceptance`); Docker must be running. Coverage report: `./mvnw -B test jacoco:report` (`target/site/jacoco`). Mutation testing: `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (`target/pit-reports`).

## Check

```bash
./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check
```

Format (google-java-format via Spotless; `./mvnw spotless:apply` fixes it), lint and duplication (PMD, CPD), static analysis (SpotBugs). Dependency scan: `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` with `NVD_API_KEY` exported.

## Troubleshooting

- Tests fail with "Could not find a valid Docker environment": start Docker; Testcontainers needs it.
- The application stops with "Invalid conference configuration: …": the file in `CONFERENCE_CONFIG_FILE` breaks `conference-config.schema.json` (duplicate id, unknown category, missing name); the message names the problem.
- The application stops with "reCAPTCHA test mode is on in production" or "reCAPTCHA keys are empty": set `APP_ENVIRONMENT=local` for local test mode, or provide both keys.
- "Could not resolve placeholder 'MAIL_HOST'" (or another variable): a required setting is missing.
- Export answers 403 although the login is right: organizer access is HTTPS-only; use HTTPS through the proxy, or `ORGANIZER_HTTPS_ONLY=false` locally.
