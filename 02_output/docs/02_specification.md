# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Contracts (normative, `docs/02_contracts/`): `openapi.yaml` (REST), `registration-copy.schema.json` (JSON copy and attachment), `conference-config.schema.json` with the shipped instance `conference.json` (options and consents), `database.sql` (PostgreSQL schema), `emails.md` (SMTP), `ui-contract.md` (registration page). Section numbers below (§n) are referenced from the traceability table.

## §1 Components and runtime topology

| Component | Folder | Runs as | Port inside | Local exposure |
|---|---|---|---|---|
| backend | `02_output/backend` | Spring Boot jar in `eclipse-temurin` JRE image, user `app` (uid 10001) | 8080 | none (reached through the frontend's `/api` proxy) |
| frontend | `02_output/frontend` | static build served by `nginx` (multi-stage: `node` build stage), user `nginx` (uid 101) | 8080 | `127.0.0.1:8080` |
| postgres | image | PostgreSQL 16, database and user `registration`, password `POSTGRES_PASSWORD` | 5432 | none |
| mailpit | image | SMTP 1025, HTTP API/UI 8025 | 1025, 8025 | `127.0.0.1:8025` (API and UI) |

`02_output/docker-compose.yml` (project name `registration`) starts the four services with `docker compose --env-file ../.env up -d --build` from `02_output/`; the backend jar is built first with `./mvnw -B package -DskipTests` (the image copies `target/*.jar`; no JDK image is in `tech-stack.md`). Named volumes: `pgdata` (database) and `jsoncopies` (mounted at `/data/json-copies` in the backend) survive `docker compose down` and `up` (NFR-02). The shipped `backend/config/conference.json` (identical to `docs/02_contracts/conference.json`) is mounted read-only at `/config/conference.json`. Health checks: backend `wget -qO- http://127.0.0.1:8080/actuator/health/readiness`, postgres `pg_isready`, frontend `wget -qO- http://127.0.0.1:8080/`; the backend starts after postgres is healthy, the frontend after the backend is healthy (NFR-04, ES-09).

Production (`environments.md`): the same two images behind the external nginx, which terminates TLS, serves `/` from the frontend and forwards `/api` to the backend, setting `Host` to the upstream itself and `X-Forwarded-For $remote_addr`, `X-Forwarded-Proto $scheme` (SR-08). The frontend container's own nginx also proxies `/api` to `http://backend:8080` with the same header rules, so the local stack behaves like production.

## §2 Backend architecture (AR-02, AR-03)

Ports-and-adapters, root package `si.konferenca.registration`. Declared layers:

| Package | Contains | May depend on |
|---|---|---|
| `domain` | registration model, option catalogue, consent definitions, validation rules, value types; pure Java | Java SE only |
| `application` | use cases (`RegisterParticipant`, `GetFormConfiguration`, `ExportRegistrations`) and the ports they need (`RegistrationStore`, `JsonCopyStore`, `CaptchaVerifier`, `RegistrationNotifier`, `WorkbookWriter`) | `domain`, Spring stereotype and transaction annotations |
| `adapter.web` | REST controllers, request/response DTOs, error mapping, servlet filters (rate limit, body size, organizer HTTPS-only), Spring Security configuration | `application`, `domain`, `config` |
| `adapter.persistence` | JPA entities, Spring Data repositories, `RegistrationStore` implementation | `application`, `domain` |
| `adapter.file` | `JsonCopyStore` implementation (atomic file write) | `application`, `domain` |
| `adapter.mail` | `RegistrationNotifier` implementation (Spring Mail) | `application`, `domain` |
| `adapter.captcha` | `CaptchaVerifier` implementations (Google siteverify, test mode) | `application`, `domain`, `config` |
| `adapter.export` | `WorkbookWriter` implementation (Apache POI) | `application`, `domain` |
| `config` | `@ConfigurationProperties`, conference configuration loader, production safety check | `application`, `domain` |
| root | `RegistrationApplication` (entry point only) | everything |

Reason: the domain rules (BR-01 … BR-05, SR-04) and the storage order (AR-05) are the risky parts; isolating them from Spring MVC, JPA, SMTP and POI makes them testable without containers and keeps each external system replaceable (production vs. test reCAPTCHA).

ArchUnit rules (`architecture` test level, phase 5; each rule names this section): (A1) `domain` depends on no other project package and on no `org.springframework`, `jakarta.persistence`, `org.apache.poi` class; (A2) `application` depends only on `domain` among project packages; (A3) no `adapter.*` package depends on another `adapter.*` package; (A4) only `adapter.persistence` uses `jakarta.persistence` and Spring Data; only `adapter.export` uses `org.apache.poi`; only `adapter.mail` uses `org.springframework.mail`/`jakarta.mail`; (A5) slices `si.konferenca.registration.(*)..` are free of cycles (AR-03).

## §3 Configuration (ES-01, AR-04, SR-02)

Settings come from environment variables (Spring relaxed binding); secrets have no default. "Required" settings have no default in `application.yml`: the backend does not start without them.

| Variable | Property | Default | Notes |
|---|---|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME` | `spring.datasource.url`, `.username` | none (required) | local compose sets them |
| `POSTGRES_PASSWORD` | `spring.datasource.password` | none (secret) | |
| `SMTP_HOST`, `SMTP_PORT` | `spring.mail.host`, `.port` | none (required) | Mailpit locally |
| `SMTP_STARTTLS` | `spring.mail.properties.mail.smtp.starttls.enable` (+ `.required`) | `true` | `false` only for Mailpit |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username`, `.password` | empty | secrets; production only |
| `MAIL_FROM` | `app.mail.from` | `registration@konferenca.local` | |
| `ORGANIZER_EMAILS` | `app.mail.organizer-recipients` | none (required, ≥ 1 valid address) | secret-listed |
| `CONFERENCE_NAME` | `app.conference.name` | `Conference` | shown in emails and the form |
| `CONFERENCE_CONFIG` | `app.conference.config-location` | none (required) | Spring resource location, e.g. `file:/config/conference.json` |
| `JSON_COPY_DIR` | `app.storage.json-copy-dir` | `/data/json-copies` | created on first write if missing |
| `RECAPTCHA_TEST_MODE` | `app.recaptcha.test-mode` | `false` | `true` only in test and local |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | `app.recaptcha.site-key`, `.secret-key` | empty | secrets |
| `RECAPTCHA_VERIFY_URL` | `app.recaptcha.verify-url` | `https://www.google.com/recaptcha/api/siteverify` | tests point it at a stub (DoD-P05) |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.username`, `.password` | none (required; password ≥ 16 chars) | password kept only as a BCrypt hash after start-up |
| `ORGANIZER_HTTPS_ONLY` | `app.organizer.https-only` | `true` | `false` only on local plain HTTP |
| `CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins` | empty (no CORS) | local Vite dev server only |
| `RATE_LIMIT_FORM_CONFIG_PER_MINUTE` | `app.rate-limit.form-config-per-minute` | `60` | per client IP |
| `RATE_LIMIT_REGISTRATIONS_PER_MINUTE` | `app.rate-limit.registrations-per-minute` | `10` | per client IP |
| `RATE_LIMIT_EXPORT_PER_MINUTE` | `app.rate-limit.export-per-minute` | `10` | per client IP, failed logins included |
| `REQUEST_MAX_BODY_BYTES` | `app.request.max-body-bytes` | `16384` | |
| `SPRING_PROFILES_ACTIVE` | | none | `prod` in production |

**Production safety check (SR-02):** with profile `prod`, start-up fails when `app.recaptcha.test-mode` is true, when either reCAPTCHA key is blank, or when `app.organizer.https-only` is false.

**Conference configuration (AR-04, US-003):** a JSON file matching `conference-config.schema.json`, read once at start-up from `CONFERENCE_CONFIG`. The loader rejects (start-up fails with a message naming the problem, AC-003-03): unreadable file, invalid JSON, unknown property, missing or blank `name` or consent `text`, unknown `category` or registration type, id not matching the pattern, duplicate option id, duplicate consent id, empty `availableTo`. `active` defaults to `true`, `availableTo` to both types. Changing options means editing this file and restarting; the participant fields are code and never configurable (BR-01). The shipped file has active options in all four categories for external participants, options available to one type only, and one inactive option (`docs/02_contracts/conference.json`).

## §4 Registration use case (US-001, US-002, US-004 … US-007)

1. **Parse** (`adapter.web`): JSON body ≤ `REQUEST_MAX_BODY_BYTES` (else 413), `Content-Type: application/json` (else 415). Malformed JSON or an unknown property → 400 with field `body`.
2. **Normalise:** every text value is trimmed (BR-02); `null` option list → empty; `null` consents → empty.
3. **Validate** (`domain`, all errors collected, SB-01), messages exact:

| Field | Rule | Message |
|---|---|---|
| type | present, `EXTERNAL` or `STUDENT` | `Choose a registration type.` (then no type-specific checks) |
| firstName, lastName, email, organization (EXTERNAL), studyInstitution, studyProgramme, studentId (STUDENT) | non-blank after trimming | `This field is required.` |
| same | length ≤ 100 (names), 254 (email), 200 (organization, study institution, study programme), 50 (student ID) | `Must be at most <n> characters.` |
| email | matches `^[^\s@]+@[^\s@]+\.[^\s@]+$` | `Enter a valid email address.` |
| fields of the other type | absent, null or blank | `Not allowed for this registration type.` |
| options | ≤ 50 ids; each id known, active and available to the type (BR-04, SR-04, OQ-01); no id twice (OQ-04) | `Option '<id>' is not available.` / `Option '<id>' is selected more than once.` / `Too many options.` (field `options`, one error per problem) |
| consents.&lt;id&gt; | every mandatory consent present with `true` (BR-05) | `This consent is required.` |
| consents.&lt;id&gt; | id is a configured consent | `Unknown consent.` |
| recaptchaToken | non-blank | `Please confirm that you are not a robot.` |

Text otherwise accepts any Unicode, including line breaks and markup (AC-006-02); it is never interpreted.
4. **Verify the token** (only if step 3 found no error; SR-01): `CaptchaVerifier`. Production: `POST RECAPTCHA_VERIFY_URL` form-encoded `secret`, `response` (and `remoteip`), timeout 5 s; accepted only if the JSON answer has `"success": true`. Any other answer, non-200 status or I/O error → rejected. Test mode: accepted only for the token `test-pass`. Rejected → 400, field `recaptchaToken`, `Anti-automation check failed. Please try again.`
5. **Store** (`application`, one database transaction, AR-05, BR-07): generate a random UUID and `registeredAt` (UTC, truncated to milliseconds; consents `givenAt` = `registeredAt`); insert the rows (`database.sql`); serialise the JSON copy (`registration-copy.schema.json`, field order as in the schema, UTF-8) and write it to `<JSON_COPY_DIR>/<id>.json` via a temporary file and an atomic move; then commit. If the JSON write fails, the transaction rolls back and the API answers 500 `Registration could not be stored. Please try again later.` (AC-005-03). If the commit fails after the file was written, the file is deleted.
6. **Answer** 201 with `RegistrationAccepted` and `Location`.
7. **Notify** after commit (OQ-03, `emails.md`): participant confirmation, then one organizer notification per address with the stored JSON bytes attached. Exceptions are caught and logged with the registration id only (ES-07).

Second registrations with the same email are stored separately (OQ-05). Retention: no automatic deletion (OQ-06); the release notes carry the 12-month deletion instruction.

## §5 Form configuration (US-003)

`GET /api/form-config` returns `conferenceName`, the active options in configuration order with `availableTo` (AC-003-01), the consents, and `captcha` = `{mode: "test", siteKey: null}` in test mode or `{mode: "recaptcha", siteKey}` otherwise (AR-07). The response is computed from the configuration loaded at start-up, so a restart with another file changes it with no code change (AC-003-02).

## §6 Export (US-008)

`GET /api/registrations/export`, organizer only (BR-08, SB-02):

- Filter order: rate limit → organizer HTTPS-only check → Basic authentication → controller. HTTPS-only: when `app.organizer.https-only` is true and the request is not secure (after `X-Forwarded-Proto` from a trusted proxy) and the client address is not loopback, answer 403 `{"message":"Organizer access requires HTTPS."}` (SR-06, D-10).
- Authentication: Spring Security HTTP Basic, one in-memory user from `ORGANIZER_USERNAME` with the BCrypt hash of `ORGANIZER_PASSWORD` (SB-03), role `ORGANIZER`. Missing or wrong credentials → 401 `{"message":"Authentication required."}` with `WWW-Authenticate: Basic realm="organizer"`, no data (AC-008-02).
- Workbook (`.xlsx`, POI `XSSFWorkbook`), sheet `Registrations`, header row then one row per registration ordered by `registeredAt` (AC-008-01). Columns: `Registration ID`, `Type` (`EXTERNAL`/`STUDENT`), `Registered at` (ISO-8601 UTC), `First name`, `Last name`, `Email`, `Organization / institution`, `Study institution`, `Study programme`, `Student ID`, `Workshops`, `Events`, `Meals`, `Other activities` (selected option names of that category joined by `; `), `Consents` (`<id> (<givenAt>)` joined by `; `). Every cell is a string cell, so text is unchanged (NFR-01, AC-008-03) and never evaluated as a formula. Only registration data (SR-07).
- Headers: `Content-Disposition: attachment; filename="registrations.xlsx"`, `Cache-Control: no-store`.

## §7 Web security (SB, SR)

- Spring Security: stateless, no session, CSRF off (no cookies or sessions are used; the only authenticated call is a read with Basic credentials). Permit `GET /api/form-config`, `POST /api/registrations`, `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`; export as §6; everything else denied (403). Actuator exposes `health` only, without details.
- Headers on every backend response (SB-10): `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`.
- Frontend nginx headers (SB-10): `Content-Security-Policy: default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; connect-src 'self'; img-src 'self' data:; style-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`; `client_max_body_size 16k`; `server_tokens off`; `proxy_set_header Host $proxy_host` (SR-08).
- Rate limit (SR-03, SB-06): servlet filter before Spring Security, fixed one-minute window per (client IP, group) with the limits of §3; over the limit → 429 `{"message":"Too many requests. Please try again later."}` with `Retry-After`. Client IP is `request.getRemoteAddr()` after Tomcat's `RemoteIpValve` (`server.forward-headers-strategy: native`, trusting only private and loopback proxy addresses).
- Body size (SR-03): filter rejects a declared or actual body above the limit with 413 `{"message":"Request too large."}`.
- Errors (SB-07, ES-07): one `@RestControllerAdvice`; unexpected exceptions → 500 `{"message":"An unexpected error occurred."}`; `server.error.include-*` never; logs carry registration ids, never names, emails, tokens or credentials.
- CORS: only the origins in `CORS_ALLOWED_ORIGINS`, methods GET/POST, no credentials.
- SQL only through JPA with bound parameters (SB-05).

## §8 Frontend (US-001, US-002, US-004, NFR-03)

React single page as defined in `ui-contract.md`: modules `api` (fetch calls and response parsing), `validation` (client rules mirroring §4 required/email/consent/token rules), `RegistrationForm`, `OptionGroups`, `Consents`, `Captcha` (test checkbox or Google widget loaded with `https://www.google.com/recaptcha/api.js?render=explicit`), `Confirmation`. All server text is rendered as React text (no `dangerouslySetInnerHTML`, SB-05). Only relative `/api` URLs (AR-01).

## §9 Test levels and harness

| Level | Component | Location | Harness |
|---|---|---|---|
| acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | `@SpringBootTest(webEnvironment = RANDOM_PORT)` over real HTTP (`java.net.http.HttpClient`), PostgreSQL and Mailpit through Testcontainers (`postgres:16.15-alpine`, `axllent/mailpit:v1.31.1`, Mailpit API for received mail), reCAPTCHA test mode, test configuration files under `src/test/resources/acceptance/`; database checked with `JdbcTemplate`, JSON copies on a temporary directory |
| acceptance | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library + jsdom, `App` rendered, global `fetch` stubbed per `openapi.yaml` |
| e2e | all | `frontend/e2e/` (`playwright.config.ts`, `testDir: e2e`) | Playwright Chromium against `http://127.0.0.1:8080` (local stack, `RECAPTCHA_TEST_MODE=true`, `ORGANIZER_HTTPS_ONLY=false`); database via `docker compose exec -T postgres psql -U registration -d registration`, JSON copies via `docker compose exec -T backend ls/cat /data/json-copies`, mail via Mailpit API `http://127.0.0.1:8025/api/v1/`, export via Playwright `request` with `ORGANIZER_USERNAME`/`ORGANIZER_PASSWORD` from the environment |
| unit, integration, architecture | each | other test paths | phase 5 |

The local stack for e2e runs with `RECAPTCHA_TEST_MODE=true`, `ORGANIZER_HTTPS_ONLY=false`, `SMTP_STARTTLS=false`, `CONFERENCE_NAME=Konferenca 2026` and `CONFERENCE_CONFIG=file:/config/conference.json`.

## §10 Non-functional requirements

| NFR | Design |
|---|---|
| NFR-01 | UTF-8 end to end: PostgreSQL `UTF8`, JSON copy UTF-8 without escaping non-ASCII, `text/plain; charset=UTF-8` mail, xlsx string cells, `lang` and `<meta charset="UTF-8">` in the page |
| NFR-02 | named volumes `pgdata`, `jsoncopies` (§1); demonstrated in phase 6 with `docker compose down` / `up` |
| NFR-03 | `ui-contract.md` client rules and backend error placement |
| NFR-04 | actuator liveness and readiness (database included) and compose health checks (§1) |

## Traceability

| AC / SR / AR / SB / NFR | Section |
|---|---|
| AC-001-01, AC-002-01 | §4 steps 1-6, `openapi.yaml` `createRegistration` |
| AC-001-02, AC-002-02 | §4 step 3 (required) |
| AC-001-03 | §4 step 2 |
| AC-001-04, AC-002-03 | §4 step 3 (email) |
| AC-001-05, AC-002-04 | §4 step 3 (options), §3 conference configuration |
| AC-001-06, AC-002-05 | §4 step 3 (consents) |
| AC-001-07, AC-002-06 | §4 steps 3-4 |
| AC-001-08, AC-002-07 | §8, `ui-contract.md` (type choice, fields, options, consents) |
| AC-003-01, AC-003-02 | §5 |
| AC-003-03 | §3 conference configuration |
| AC-004-01 … AC-004-04 | §8, `ui-contract.md` (states, field errors) |
| AC-005-01, AC-005-04 | §4 step 5, `database.sql` |
| AC-005-02 | §4 step 5, `registration-copy.schema.json` |
| AC-005-03 | §4 step 5 (rollback) |
| AC-006-01 … AC-006-03 | §4 step 7, `emails.md` |
| AC-007-01, AC-007-02 | §4 step 7, `emails.md` |
| AC-008-01, AC-008-03 | §6 |
| AC-008-02 | §6 authentication |
| AC-008-04 | §6 HTTPS-only, D-10 |
| SR-01 | §4 step 4 |
| SR-02 | §3 production safety check |
| SR-03 | §7 rate limit, body size |
| SR-04 | §4 step 3 (options) |
| SR-05 | `emails.md` common rules, §4 step 3 |
| SR-06 | §6 HTTPS-only |
| SR-07 | §6 workbook, `emails.md` |
| SR-08 | §1 production, §7 frontend nginx |
| AR-01 | §8, `ui-contract.md` network |
| AR-02, AR-03 | §2 |
| AR-04 | §3 conference configuration, §5 |
| AR-05 | §4 step 5 |
| AR-06 | `database.sql`, Flyway `V1__create_registration_tables.sql` |
| AR-07 | §5 `captcha.siteKey`, `ui-contract.md` |
| SB-01 | §4 step 3 |
| SB-02 | §6, §7 |
| SB-03 | §6 BCrypt; §3 secrets from configuration |
| SB-04 | §1 production TLS at the external proxy; §6 HTTPS-only; SMTP STARTTLS default on |
| SB-05 | §7 SQL, §8 React text, `emails.md` plain text |
| SB-06 | §7 rate limit |
| SB-07 | §7 errors |
| SB-08, SB-09 | phase 6 scans (Dependency-Check, npm audit, Semgrep, gitleaks) |
| SB-10 | §7 headers |
| SB-11 | §1 non-root users |
| SB-12, SB-13 | only the fields of BR-01 Data are collected; purpose and retention in `security-requirements.md` and OQ-06 |
| SB-14 | `ui-contract.md` consents unchecked, §4 step 5 consent with `givenAt` |
| NFR-01 … NFR-04 | §10 |

## Contract validation

| Contract | Parser | Result |
|---|---|---|
| `openapi.yaml` | Redocly CLI 2.58.2 (container, `lint`, D-11) | valid, 0 warnings (`logs/02_openapi-lint.log`) |
| `conference-config.schema.json`, `registration-copy.schema.json` | Ajv 8.20.0 (draft-07; already in the frontend's dev dependency tree through ESLint, D-11) | both compile; `conference.json` valid; invalid samples rejected (`logs/02_schema-validation.log`) |
| `database.sql` | PostgreSQL 16.15 (`psql -v ON_ERROR_STOP=1` in a throw-away `postgres:16.15-alpine` container) | applied, 3 tables (`logs/02_sql-validation.log`) |
| `emails.md`, `ui-contract.md` | prose contracts, no parser | checked by the acceptance tests |
