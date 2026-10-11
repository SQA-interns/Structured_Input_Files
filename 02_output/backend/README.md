# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

Spring Boot REST API of the conference registration (`si.konferenca.registration`). Layers and rules: `docs/02_specification.md` section 2.

## Prerequisites

- Eclipse Temurin JDK 21.0.10 (`java -version`). Maven is not needed: `./mvnw` downloads Apache Maven 3.9.9.
- Docker Engine (tests start PostgreSQL 16.15 and Mailpit with Testcontainers; the image runs in Docker).

## Configuration

All settings are Spring properties; in containers set them as environment variables. Full table: `docs/02_specification.md` section 3.

| Setting | Environment variable | Source | Default |
|---|---|---|---|
| Environment (`production`, `local`, `test`) | `APP_ENVIRONMENT` | deployment | `production` |
| Database URL and user | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | deployment | none |
| Database password | `POSTGRES_PASSWORD` | secret, `.env` | none |
| SMTP host, port, TLS | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | deployment | none |
| SMTP account | `SMTP_USERNAME`, `SMTP_PASSWORD` | secret, `.env` (production only) | none |
| Sender address, conference name | `APP_MAIL_SENDER`, `APP_CONFERENCENAME` | deployment | `registration@konferenca.si`, `Conference` |
| Organizer login and recipients | `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` (≥ 16 characters), `ORGANIZER_EMAILS` (comma separated) | secret, `.env` | none |
| Organizer HTTPS-only access | `APP_ORGANIZER_HTTPSONLY` | deployment | `true` (production refuses `false`) |
| Options and consents file | `APP_OPTIONS_LOCATION` (e.g. `file:/config/conference-options.json`) | deployment | `classpath:conference-options.json` (refused in production) |
| JSON copy directory | `APP_JSONCOPY_DIRECTORY` | deployment (persistent volume) | `/data/json-copies` |
| reCAPTCHA keys | `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | secret, `.env` (production) | none |
| reCAPTCHA test mode (token `test-pass`) | `APP_RECAPTCHA_TESTMODE` | local and test only | `false` (production refuses `true`) |
| CORS origin for local Vite development | `APP_CORS_ALLOWEDORIGIN` | local only | empty |
| Rate limits, body size | `APP_RATELIMIT_REGISTRATION_MAXREQUESTS`, `..._WINDOW`, `APP_RATELIMIT_EXPORT_*`, `APP_RATELIMIT_PUBLICAPI_*`, `APP_REQUEST_MAXBODYBYTES` | deployment | 10 / 10 min, 20 / 10 min, 120 / 1 min, 16384 bytes |

The backend refuses to start (and names the setting, never its value) when a required secret is missing or a production setting is unsafe.

## Build

```bash
./mvnw -B -DskipTests package
```

The jar is `target/registration-backend-0.1.0.jar`; the container image is built from it: `docker build -t registration-backend .`

## Run

With the whole local stack (recommended), from `02_output/`:

```bash
docker compose --env-file ../.env up -d --build --wait backend
```

The API listens on http://127.0.0.1:8080 (`/api/...`, health on `/actuator/health/liveness` and `/actuator/health/readiness`).

## Test

```bash
./mvnw -B verify
```

Runs unit, integration, architecture (ArchUnit) and acceptance tests (Testcontainers; Docker must run) and writes JaCoCo coverage to `target/site/jacoco/`. Mutation testing: `./mvnw -B test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage` (about 30 minutes; report in `target/pit-reports/`). Dependency scan: `NVD_API_KEY` in the environment, then `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check`.

## Check

```bash
./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check
```

Format (google-java-format via Spotless; fix with `./mvnw spotless:apply`), PMD, copy-paste detection and SpotBugs.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Refusing to start: …` in the log | A required setting is missing or unsafe for production; set the named variable. |
| `Invalid conference options configuration: … option 'x' …` | The options file breaks `conference-options.schema.json`; fix the named entry. |
| Tests fail with `Could not find a valid Docker environment` | Start Docker; Testcontainers needs it. |
| Registrations return 500 and the log shows an error id | Database or JSON copy directory unavailable; nothing was stored. Check `/actuator/health/readiness` and the volume. |
| Export answers 403 `HTTPS_REQUIRED` | Organizer access over plain HTTP from another machine; use HTTPS through the reverse proxy (sets `X-Forwarded-Proto`). |
| 429 `TOO_MANY_REQUESTS` | Per-client rate limit; wait for the `Retry-After` seconds or raise the limit. |
