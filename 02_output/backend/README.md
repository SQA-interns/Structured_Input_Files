# Backend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

Spring Boot 4.1.1 REST API (Java 21), root package `si.konferenca.registration`. Endpoints: `GET /api/form-config`, `POST /api/registrations`, `GET /api/export` (organizer, HTTP Basic); health under `/actuator/health`. Contract: [../docs/02_contracts/openapi.json](../docs/02_contracts/openapi.json).

## Prerequisites

- JDK 21 (Temurin 21.0.10); Maven is downloaded by the wrapper (`./mvnw`, Maven 3.9.9).
- Docker Engine for the tests (Testcontainers starts PostgreSQL 16 and Mailpit) and for the container image.

## Configuration

All settings come from environment variables (ES-01); defaults exist only where `environments.md` allows them. Secrets are listed in `01_input/01_project/00_setup/secrets.env.example` and kept in the repository-root `.env`.

| Variable | Meaning | Default |
|---|---|---|
| `DB_URL`, `DB_USER` | JDBC URL and user | local values (`jdbc:postgresql://localhost:5432/registration`, `registration`) |
| `POSTGRES_PASSWORD` | database password (secret) | none — required |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS`, `SMTP_AUTH` | mail server | `localhost`, `1025`, `false`, `false` (Mailpit) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | SMTP account (secret, production) | empty |
| `MAIL_FROM` | sender address | `registration@konferenca.si` |
| `CONFERENCE_NAME` | name in emails and page title | `Conference` |
| `CONFERENCE_CONFIG` | options and consents file (Spring resource, e.g. `file:/config/conference.json`) | `classpath:conference.json` |
| `JSON_COPY_DIR` | directory of the JSON copies (persistent volume) | `./data/json-copies` |
| `APP_ENVIRONMENT` | `production`, `local` or `test` | `production` |
| `RECAPTCHA_TEST_MODE` | deterministic test mode (only `local`/`test`) | `false` |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | Google reCAPTCHA v2 keys (secret) | none — required when test mode is off |
| `RECAPTCHA_VERIFY_URL` | verification endpoint | Google `siteverify` |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | organizer login (secret) | none — required |
| `ORGANIZER_EMAILS` | notification recipients, comma separated | none — required |
| `ORGANIZER_HTTPS_ONLY` | refuse the export over plain HTTP from non-local clients | `true` |
| `APP_CORS_ALLOWED_ORIGIN` | allowed browser origin (local dev server only) | empty (CORS off) |
| `RATE_LIMIT_FORM_CONFIG`, `RATE_LIMIT_REGISTRATIONS`, `RATE_LIMIT_EXPORT`, `MAX_BODY_BYTES` | per-client limits per minute, body limit | `60`, `10`, `10`, `16384` |

The application refuses to start with an invalid options file, with test mode outside `local`/`test`, with missing reCAPTCHA keys while test mode is off, or with missing organizer settings.

**Options and consents** are configured in a JSON file ([schema](../docs/02_contracts/conference-config.schema.json)); the shipped example is `src/main/resources/conference.json`. Change the file and restart; no code change is needed.

## Build, test, check, run (ES-05)

| Task | Command (in `02_output/backend`) |
|---|---|
| build | `./mvnw -B -DskipTests package` |
| test (all levels, coverage report in `target/site/jacoco`) | `./mvnw -B test` |
| check (format, lint, duplication, static analysis) | `./mvnw -B -DskipTests compile spotless:check pmd:check pmd:cpd-check spotbugs:check` |
| run (needs PostgreSQL and the variables above) | `./mvnw spring-boot:run` or `java -jar target/registration-backend-1.0.0.jar` |
| mutation testing | `./mvnw -B test-compile org.pitest:pitest-maven:1.30.0:mutationCoverage` |
| dependency scan (needs `NVD_API_KEY`) | `./mvnw -B org.owasp:dependency-check-maven:12.1.0:check` |
| format code | `./mvnw spotless:apply` |

Container image: `docker build -t registration-backend .` after `package`; runs as user 10001, JSON copies in `/data/json-copies`.

## Production reverse proxy

The backend runs behind an external nginx that terminates TLS (Let's Encrypt), serves `/` from the frontend container and forwards `/api` to the backend. It must set the upstream `Host` itself (SR-08) and pass the scheme so the HTTPS-only export check works:

```nginx
location /api/ {
    proxy_pass http://backend:8080;
    proxy_set_header Host backend;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    client_max_body_size 32k;
}
```

Production settings: `APP_ENVIRONMENT=production`, `RECAPTCHA_TEST_MODE=false`, real reCAPTCHA keys, SMTP host and account, `ORGANIZER_HTTPS_ONLY=true`.

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Could not resolve placeholder 'ORGANIZER_USERNAME'` (or `POSTGRES_PASSWORD`) at startup | the variable is missing; set it in `.env` / the environment |
| `Invalid conference configuration: …` | the options file violates the schema (duplicate id, unknown category, missing name); fix the file |
| `reCAPTCHA test mode is only allowed …` | set `APP_ENVIRONMENT=local` locally, or turn test mode off and set the keys |
| tests fail with Docker errors | Docker must run; Testcontainers needs access to the Docker engine |
| export answers 403 locally | `ORGANIZER_HTTPS_ONLY=true` and the request did not come from localhost; the local compose stack sets it to `false` |
| export answers 401 | wrong organizer username or password |
| registration answers 429 | rate limit reached; wait a minute |
