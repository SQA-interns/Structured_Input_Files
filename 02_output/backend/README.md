# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API (root package `si.konferenca.registration`): form configuration, registrations, Excel export, emails, JSON copies. Architecture and contracts: `../docs/02_specification.md`, `../docs/02_contracts/`.

## Prerequisites

- JDK 21 (Temurin); Maven comes with the wrapper (`./mvnw`, Maven 3.9.9).
- Docker for the tests (Testcontainers starts PostgreSQL and Mailpit) and for the container image.

## Configuration

Every setting is an environment variable (spec section 13); `src/main/resources/application.properties` maps each one. Secrets come from `.env` (template `01_input/01_project/00_setup/secrets.env.example`), never from code.

| Variable | Secret | Default (local/test only) |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USER` | no | `jdbc:postgresql://localhost:5432/registration`, `registration` |
| `POSTGRES_PASSWORD` | yes | none |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS` | no | `localhost`, `1025`, `false` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | yes | empty |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | yes | none |
| `ORGANIZER_HTTPS_ONLY` | no | `true` (set `false` only for local plain HTTP) |
| `RECAPTCHA_TEST_MODE` | no | `false` (`true` locally and in tests) |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | yes | empty |
| `RECAPTCHA_VERIFY_URL` | no | Google siteverify |
| `REGISTRATION_CONFERENCE_NAME`, `REGISTRATION_MAIL_FROM` | no | `Conference`, `registration@konferenca.si` |
| `REGISTRATION_CONFERENCE_CONFIG` | no | `classpath:conference.json` (options and consents, schema `../docs/02_contracts/conference-config.schema.json`) |
| `REGISTRATION_JSON_COPY_DIR` | no | `./data/json-copies` |
| `REGISTRATION_RATE_LIMIT_FORM_CONFIG`, `_REGISTRATIONS`, `_EXPORT`, `REGISTRATION_MAX_BODY_BYTES` | no | `120`, `20`, `10` per minute, `16384` |
| `REGISTRATION_CORS_ALLOWED_ORIGINS` | no | empty (local development only) |

Production: `SPRING_PROFILES_ACTIVE=prod` removes the local defaults and refuses to start with reCAPTCHA test mode on, empty reCAPTCHA keys or `ORGANIZER_HTTPS_ONLY=false`.

## Commands (ES-05)

| Purpose | Command |
|---|---|
| Build | `./mvnw -B package -DskipTests` (jar in `target/`) |
| Test | `./mvnw -B test` (unit, acceptance with Testcontainers, architecture; needs Docker) |
| Check | `./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check` |
| Run | `./mvnw spring-boot:run` with the variables above exported (or `docker compose up` in `..`) |
| Format | `./mvnw spotless:apply` |
| Coverage | `./mvnw -B test jacoco:report` (report in `target/site/jacoco`) |
| Mutation | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (report in `target/pit-reports`) |
| Dependency scan | `NVD_API_KEY=... ./mvnw org.owasp:dependency-check-maven:check` |

Health: `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Start fails with "Could not resolve placeholder 'ORGANIZER_EMAILS'" (or another secret) | the variable is not set; secrets have no default |
| Start fails with "Invalid conference configuration: …" | the options file violates its schema; the message names the option and the rule |
| "Refusing to start in production: …" | the `prod` profile found test mode on, empty reCAPTCHA keys or HTTPS-only off |
| Tests fail with Docker or Testcontainers errors | start Docker; the tests start `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1` |
| Export answers 403 "HTTPS is required." | plain HTTP from a non-local client; use HTTPS through the proxy or, locally only, `ORGANIZER_HTTPS_ONLY=false` |
| Registrations answer 429 | the per-client rate limit; wait for the `Retry-After` seconds or raise the limit variables |
