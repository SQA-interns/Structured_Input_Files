# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API (root package `si.konferenca.registration`). Architecture and rules: `../docs/02_specification.md` (S2–S12); API: `../docs/02_contracts/openapi.yaml`.

## Prerequisites

- JDK 21 (pinned: Eclipse Temurin 21.0.10+7; OpenJDK 21.0.12 approved in D-07)
- Docker (tests use Testcontainers with `postgres:16.15-alpine` and `axllent/mailpit:v1.31.1`)
- Maven is not needed: `./mvnw` downloads Maven 3.9.9 (checksum pinned)
- Tests must not run as root (one test makes a directory read-only)

## Commands (ES-05)

| Purpose | Command |
|---|---|
| build | `./mvnw -B package -DskipTests` → `target/registration-backend-0.1.0.jar` |
| test | `./mvnw -B verify` (all levels; coverage in `target/site/jacoco/`) |
| check | `./mvnw -B compile spotless:check pmd:check pmd:cpd-check spotbugs:check` (format with `./mvnw spotless:apply`) |
| run | with the local stack: `../scripts/compose.sh up -d --build backend` (after `build`); standalone: `java -jar target/registration-backend-0.1.0.jar` with the variables below |
| mutation | `./mvnw -B test-compile org.pitest:pitest-maven:mutationCoverage` (unit and architecture tests, D-23) |
| dependency scan | `NVD_API_KEY=… ./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` (pass the key from `.env` without printing it) |

## Configuration

Every environment-specific value is an environment variable (ES-01); full table in `../docs/02_specification.md` S11.

| Variable | Default | Source |
|---|---|---|
| `DB_URL`, `DB_USERNAME` | `jdbc:postgresql://localhost:5432/registration`, `registration` (not in `prod`) | environment |
| `POSTGRES_PASSWORD` | none | `.env` (secret) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS`, `MAIL_AUTH` | `localhost`, `1025`, `false`, `false` (`prod`: TLS and auth on, no host/port default) | environment |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty | `.env` (secret, production) |
| `MAIL_FROM`, `CONFERENCE_NAME` | `registration@konferenca.local`, `Conference` | environment |
| `OPTIONS_LOCATION` | `classpath:conference-options.json` (`prod`: must be `file:`) | environment; file format `../docs/02_contracts/conference-options.schema.json` |
| `JSON_COPY_DIR` | `/data/registrations` | environment (persistent volume) |
| `RECAPTCHA_TEST_MODE` | `false` (`prod` refuses `true`) | environment |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | none; required unless test mode | `.env` (secret) |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | environment |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | none | `.env` (secret) |
| `ORGANIZER_REQUIRE_HTTPS` | `true` (`false` only on local plain HTTP) | environment |
| `CORS_ALLOWED_ORIGINS` | empty | environment (local only) |
| `RATE_LIMIT_REGISTRATIONS_PER_MINUTE`, `RATE_LIMIT_READS_PER_MINUTE`, `RATE_LIMIT_ORGANIZER_PER_MINUTE`, `MAX_REQUEST_BYTES` | 10, 120, 10, 16384 | environment |
| `SPRING_PROFILES_ACTIVE` | none; `prod` in production | environment |

Conference options change by editing the option file and restarting; an invalid file stops the startup with a message naming the entry (D-20).

Health: `/actuator/health/liveness`, `/actuator/health/readiness` (database and JSON copy directory).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| startup fails with `Invalid configuration: …` | a required variable is missing or unsafe (see the message); set it |
| startup fails naming `option[n] '<id>'` | the option file is invalid; fix that entry |
| `Could not resolve placeholder 'POSTGRES_PASSWORD'` | the secret is not set in the environment |
| tests fail with Docker errors | Docker is not running or not reachable by the current user |
| AC-005-03 test fails | tests run as root; run as a normal user |
| readiness `DOWN` | database unreachable or `JSON_COPY_DIR` not writable by uid 1000 |
