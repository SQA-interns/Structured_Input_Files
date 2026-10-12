# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Conference registration: an anonymous participant registers as external participant or student through a React form; the Spring Boot backend validates, stores (PostgreSQL + raw JSON copy), confirms by email and notifies organizers; organizers export an Excel workbook. Versions: `tech-stack.md` only. Contracts: `docs/02_contracts/`.

## 1. System context

```
browser ──HTTP──> frontend (nginx: static files, /api proxy) ──HTTP──> backend (Spring Boot, :8080)
                                                                         ├─ JDBC ──> PostgreSQL
                                                                         ├─ file ──> JSON copy volume
                                                                         ├─ SMTP ──> mail server (Mailpit locally)
                                                                         └─ HTTPS ─> Google reCAPTCHA siteverify
production: external nginx (TLS, Let's Encrypt) ──> frontend "/" and backend "/api"
```

- S1.1 The frontend calls the backend only under `/api` (AR-01): `GET /api/form-config`, `POST /api/registrations`, `GET /api/export`. Relative URLs; the same origin is served by nginx locally and in production.
- S1.2 Actuator health endpoints are under `/actuator/health/**` and are not routed by the reverse proxy; only container health checks use them.

## 2. Backend architecture (AR-02, AR-03)

Root package `si.konferenca.registration`. Layered by responsibility; dependencies point downwards only:

| Package | Responsibility | May depend on (internal) |
|---|---|---|
| `web` | REST controllers, request/response DTOs, exception handler, request filters (rate limit, body size, HTTPS-only) | `service`, `conference`, `config`, `domain` (entity types for responses only, D-15) |
| `security` | Spring Security configuration, organizer user (BCrypt), security headers, CORS | `config` |
| `service` | use cases: register, export; business-rule validation | `domain`, `conference`, `infra`, `config` |
| `infra` | adapters to the outside: JSON copy store, mail notifier, reCAPTCHA verifiers, Excel writer | `domain`, `conference`, `config` |
| `domain` | JPA entities, repository, registration type | — |
| `conference` | conference configuration model (options, consents), loader and startup validation | `config`, `domain` (`RegistrationType` only, D-15) |
| `config` | typed `@ConfigurationProperties` and startup guards | — |

Justification: one small API with three use cases; a layered split keeps web, business rules, persistence and external adapters separately testable and lets ArchUnit check the direction. ArchUnit rules (phase 3 tests, DoD-04): (a) the table above as a layered architecture; (b) `domain` and `config` depend on no other internal package; (c) no slice cycles over `si.konferenca.registration.(*)..` (AR-03); (d) controllers (`@RestController`) live only in `web`; (e) only `domain` declares `@Entity`; (f) `web` never uses `domain` repositories.

## 3. Conference configuration (US-003, AR-04, BR-04, OQ-01, OQ-02)

- S3.1 Source: a JSON file whose location is setting `app.conference.config` (env `CONFERENCE_CONFIG`, Spring resource syntax, default `classpath:conference.json` — local and test only per `environments.md`). Contract: `02_contracts/conference-config.schema.json`.
- S3.2 Content: `options[]` with `id` (stable, `^[a-z0-9][a-z0-9-]{0,63}$`), `name`, `category` (`WORKSHOP`, `EVENT`, `MEAL`, `OTHER`), `active` (default `true`), `availableTo` (subset of `EXTERNAL`, `STUDENT`; default both); `consents[]` with `id`, `text`, `mandatory`.
- S3.3 Loaded once at startup. The application refuses to start (exception during context start, non-zero exit) when the file is missing or unparsable, an option id is duplicated, a category is unknown, a name is missing or blank, `availableTo` is empty or holds an unknown type, a consent id is duplicated, or a consent text is missing (AC-003-03).
- S3.4 Changing the file and restarting changes the offered options; no code change, the participant fields are fixed in code (BR-01, AC-003-02).
- S3.5 Shipped configuration (`backend/src/main/resources/conference.json`): at least one active option per category, one option available only to external participants, one inactive option, and the single mandatory consent `privacy` with the wording from OQ-02.

## 4. REST API (contract `02_contracts/openapi.json`)

### 4.1 `GET /api/form-config` (public, AC-003-01, AC-001-08, AC-002-07)

Returns `conferenceName`, `recaptcha {siteKey, testMode}`, `consents[] {id, text, mandatory}`, and only **active** `options[] {id, name, category, availableTo[]}` in configuration order. Never returns inactive options or any secret (AR-07: the site key is the only key and comes from configuration).

### 4.2 `POST /api/registrations` (public, US-001, US-002, US-005, US-006, US-007)

Request JSON: `type` (`EXTERNAL`|`STUDENT`), `firstName`, `lastName`, `email`, `organization` (external), `studyInstitution`, `studyProgramme`, `studentId` (student), `optionIds[]`, `consents[]` (ids of consents given), `recaptchaToken`.

Processing order:

1. Body larger than `app.limits.max-body-bytes` (default 16384) → 413; malformed JSON → 400 `{message}`.
2. Field validation, all errors collected (SB-01):
   - `type` missing/unknown → error `type` (no further type-specific checks).
   - Text fields are trimmed (BR-02); a required field that is missing, empty or whitespace only → error on that field (AC-001-02, AC-002-02). Max lengths: names 100, email 254, organization/institution/programme 200, student ID 50 → error on that field.
   - `email` must match `^[^@\s]+@[^@\s]+\.[^@\s]+$` after trimming and must parse as a strict RFC 822 address; no CR/LF (BR-03, AC-001-04, AC-002-03, SR-05).
   - Fields of the other type that are present and non-blank → error on that field (D-12).
   - `optionIds`: each id must exist, be active, be available to the type, and appear once; otherwise error `optionIds` (BR-04, SR-04, OQ-01, OQ-04; AC-001-05, AC-002-04). Missing list = no options.
   - `consents`: each mandatory consent id must be present → error `consents.<id>` (BR-05, AC-001-06, AC-002-05); unknown consent id → error `consents`.
   - `recaptchaToken` missing/blank → error `recaptchaToken`.
3. If any error: 400 `{message, fieldErrors[{field, message}]}`; nothing stored, nothing sent.
4. reCAPTCHA verification (S7). Rejected or verifier unavailable → 400 with field error `recaptchaToken` (SR-01, AC-001-07, AC-002-06); nothing stored, nothing sent.
5. Persist (S5) — database row and JSON copy in one unit (AR-05).
6. Send participant confirmation and organizer notification (S6); failures are logged with the registration id only and do not change the response (OQ-03, AC-006-03).
7. 201 `{id, type, firstName, lastName, options[{id, name, category}], registeredAt}` (AC-001-01, AC-002-01).

Text is stored exactly as trimmed: Unicode (č, š, ž …) is preserved end to end; the database uses UTF-8 and JSON is written as UTF-8 (BR-03, NFR-01, AC-005-04). Line breaks and markup inside names are accepted as data (AC-006-02 presumes such submissions are accepted); they are neutralised on output (S6, S8, frontend renders text only).

### 4.3 `GET /api/export` (organizer, US-008, BR-08)

- S4.3.1 HTTPS-only (SR-06, AC-008-04, D-10): when `app.organizer.https-only` (env `ORGANIZER_HTTPS_ONLY`, default `true`) is on and the request is not secure (`request.isSecure()`, derived from `X-Forwarded-Proto` of a trusted proxy) and the client address is not loopback → 403 `{message}` before credentials are evaluated; no data.
- S4.3.2 HTTP Basic, single user `ORGANIZER_USERNAME`; the password from `ORGANIZER_PASSWORD` is BCrypt-hashed at startup and only the hash is kept (SB-03). Missing or wrong credentials → 401 with `WWW-Authenticate: Basic`, body without registration data (AC-008-02).
- S4.3.3 Success: 200, `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="registrations.xlsx"`, `Cache-Control: no-store`. See S8.

### 4.4 Errors (SB-07, ES-07)

Every error body is `{message}` or `{message, fieldErrors}` with generic text; no stack traces, exception names, SQL or paths. Unexpected errors → 500 `{"message":"The registration could not be processed. Please try again later."}` (AC-005-03, D-11). Rate limited → 429 `{message}`. Unknown routes → 404 / 401 / 403 from the security chain without internals.

## 5. Persistence (US-005, BR-07, AR-05, AR-06, ES-08)

- S5.1 Schema only through Flyway (`db/migration/V1__create_registration.sql`, contract `02_contracts/database.sql`). Tables: `registration` (id uuid, type, first_name, last_name, email, organization, study_institution, study_programme, student_id, registered_at timestamptz), `registration_option` (registration_id, option_id, option_name, category, position), `registration_consent` (registration_id, consent_id, consent_text, given_at timestamptz). Hibernate `ddl-auto=validate`.
- S5.2 Registration and consent times: one server timestamp (UTC) taken when the request is accepted; each consent's `given_at` equals it (AC-005-01).
- S5.3 Write order in one database transaction: insert rows, flush; write the JSON copy (to `<dir>/<id>.json.tmp`, then atomic move to `<dir>/<id>.json`); commit. A JSON write failure throws and rolls the transaction back (AC-005-03). A commit failure after the file was written deletes the file. The 201 response is produced only after commit (AR-05).
- S5.4 JSON copy directory: setting `app.json-copy.dir` (env `JSON_COPY_DIR`, default `./data/json-copies`), created on first write; on a named volume in Docker (NFR-02). Contract: `02_contracts/registration-copy.schema.json`. Content: the accepted registration (`id`, `type`, the fields of that type, `options[{id,name,category}]`, `consents[{id,text,givenAt}]`, `registeredAt`), UTF-8, no internal fields (SR-07).

## 6. Email (US-006, US-007, SR-05, SR-07, OQ-03; contract `02_contracts/emails.json`)

- S6.1 Sent synchronously after the commit with SMTP connect/read/write timeouts of 5 s; each send is independent; an exception is caught and logged as `Email <kind> failed for registration <id>` (no personal data).
- S6.2 Participant confirmation: To = the validated participant address only; From = setting `app.mail.from` (env `MAIL_FROM`); Subject = `Registration confirmed: <conference name>` (no user input in any header); body `text/plain; charset=UTF-8` naming the conference, the participant's first and last name and the selected option names (AC-006-01). User input appears only in the plain-text body, so markup stays literal text and line breaks cannot create headers (AC-006-02).
- S6.3 Organizer notification: one message per address in `ORGANIZER_EMAILS` (comma separated, at least one, validated at startup); Subject `New registration <id>`; plain-text body listing the submitted data; attachment `registration-<id>.json`, `application/json`, whose bytes are the bytes of the stored JSON copy (AC-007-01, AC-007-02).
- S6.4 SMTP host, port, TLS (`SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS`), credentials (`SMTP_USERNAME`, `SMTP_PASSWORD`, production only) and `CONFERENCE_NAME` come from configuration (ES-01).

## 7. Anti-automation (SR-01, SR-02, DoD-P05; contract `02_contracts/recaptcha.json`)

- S7.1 Production verifier: `POST` form `secret`, `response` (and `remoteip`) to `app.recaptcha.verify-url` (default `https://www.google.com/recaptcha/api/siteverify`), timeout 5 s; accepted only when the JSON answer has `success: true`. Any other answer, error or timeout = rejected.
- S7.2 Test mode (`RECAPTCHA_TEST_MODE`, default `false`): no network call; exactly the token `test-pass` is accepted, every other token is rejected. The frontend in test mode shows a labelled checkbox instead of the Google widget that produces this token.
- S7.3 Startup guard: setting `app.environment` (env `APP_ENVIRONMENT`, default `production`). The application refuses to start when test mode is on and the environment is not `local` or `test`, or when test mode is off and the site key or secret key is empty (SR-02).

## 8. Excel export (US-008, BR-08, SR-07, NFR-01)

One sheet `Registrations`; header row then one row per registration ordered by `registered_at`, columns: `Registration ID`, `Type`, `First name`, `Last name`, `Email`, `Organization`, `Study institution`, `Study programme`, `Student ID`, `Options` (option names joined by `; `), `Consents` (`<id> (<givenAt ISO-8601 UTC>)` joined by `; `), `Registered at` (ISO-8601 UTC). All values are string cells, so text (including leading `=`, `+`, `-`, `@`) is never evaluated as a formula and Unicode is unchanged (AC-008-01, AC-008-03). No other columns or system data (SR-07).

## 9. Cross-cutting security (SB, SR)

- S9.1 Rate limiting (SB-06, SR-03): servlet filter before security, per client IP (after trusted-proxy resolution), fixed one-minute windows: `form-config` 60, `registrations` 10, `export` 10 requests (settings `app.limits.*`; tests use higher values). Excess → 429. Failed organizer logins are covered by the export limit.
- S9.2 Body size limit (SR-03): S4.2 step 1; Tomcat `max-http-form-post-size` and `max-swallow-size` also bounded.
- S9.3 Security headers (SB-10): backend responses carry `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx sends a CSP allowing only self plus Google reCAPTCHA script/frame origins, `frame-ancestors 'none'`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`.
- S9.4 CORS: disabled unless `APP_CORS_ALLOWED_ORIGIN` is set (local dev server only).
- S9.5 CSRF: the API is stateless (no cookies, no session); CSRF protection is disabled for `/api/**`; Basic credentials are never stored by the browser app.
- S9.6 Parameterised queries only (JPA/Spring Data), output encoding: React text rendering (no `dangerouslySetInnerHTML`), plain-text emails, string cells (SB-05).
- S9.7 Logs: registration id and technical events only; no names, emails, tokens or secrets (SB-07, ES-07). Hibernate SQL logging off.
- S9.8 Containers run as non-root users (SB-11): backend user `app` (uid 10001); frontend nginx as user `nginx` on port 8080 with temp paths under `/tmp`.
- S9.9 Reverse proxy (SR-08, SB-04): the frontend nginx forwards `/api` with `proxy_set_header Host <upstream host>` (never `$http_host`), sets `X-Forwarded-For`/`X-Forwarded-Proto`; the production nginx example in the backend README does the same and terminates TLS. Backend `server.forward-headers-strategy=native`, trusted proxies = private address ranges (Tomcat default, configurable).
- S9.10 Dependency, static and secret scanning (SB-08, SB-09): Dependency-Check, npm audit, SpotBugs, PMD, Semgrep, Gitleaks in phase 6.

## 10. Personal data (SB-12 … SB-14)

Only the fields of BR-01, the selected options and consents with timestamps are collected (`security-requirements.md` "Personal data"); purpose and retention per OQ-06 — stored until the operator deletes them; release notes instruct deletion of rows and JSON copies within 12 months after the conference. Consent checkboxes are never preselected (BR-05) and the consent wording is stored with the timestamp.

## 11. Frontend (contract `02_contracts/ui-registration-form.json`)

- S11.1 Modules: `api.ts` (fetch wrappers, relative `/api` URLs), `validation.ts` (client checks), `App.tsx` (state machine: loading → form → confirmation, load error), `RegistrationForm.tsx`, `OptionGroups.tsx`, `Recaptcha.tsx`, `Confirmation.tsx`, `types.ts`. `main.tsx` only mounts the app.
- S11.2 On load: `GET /api/form-config`; failure → general error message.
- S11.3 Type choice: radio group "Registration type" with "External participant" and "Student"; nothing shown until chosen. External: First name, Last name, Email, Organization / institution. Student: First name, Last name, Email, Study institution, Study programme, Student ID. Values of the common fields are kept when switching type.
- S11.4 Options: only active options available to the chosen type, in fieldsets with legends "Workshops", "Events", "Meals", "Other activities" (empty groups not shown); checkbox per option labelled with its name. Consents: one checkbox per configured consent, labelled with its text, unchecked initially (BR-05).
- S11.5 reCAPTCHA: production loads `https://www.google.com/recaptcha/api.js` and renders the v2 widget with the site key from form-config; test mode renders a checkbox "I am not a robot (test mode)" yielding `test-pass`.
- S11.6 Submit "Register": client validation of required text fields (trimmed non-empty) and email format; errors shown next to the field (`<p id="<field>-error" role="alert">`, field `aria-invalid`, `aria-describedby`) and no request is sent (AC-004-03, NFR-03). Mandatory consents and the reCAPTCHA are validated by the backend.
- S11.7 201 → the form is removed and a confirmation "Registration confirmed" shows the participant's first and last name and the selected option names (AC-004-01, BR-06). 400 with field errors → each shown next to its field (`optionIds` next to the options, `consents.<id>` next to that consent, `recaptchaToken` next to the widget, others next to their input), values kept, no confirmation (AC-004-02). Any other status or network failure → general error "The registration could not be completed. Please try again later." without technical details, values kept (AC-004-04, SB-07).
- S11.8 The only key in the frontend is the reCAPTCHA site key, read at runtime from form-config (AR-07).

## 12. Configuration (ES-01, `environments.md`)

| Setting | Env variable | Default |
|---|---|---|
| Database URL, user | `DB_URL`, `DB_USER` | `jdbc:postgresql://localhost:5432/registration`, `registration` (local/test) |
| Database password | `POSTGRES_PASSWORD` | none (secret) |
| SMTP host, port, STARTTLS | `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS` | `localhost`, `1025`, `false` (Mailpit) |
| SMTP credentials | `SMTP_USERNAME`, `SMTP_PASSWORD` | empty (production sets them) |
| Sender | `MAIL_FROM` | `registration@konferenca.si` |
| Conference name | `CONFERENCE_NAME` | `Conference` |
| Options source | `CONFERENCE_CONFIG` | `classpath:conference.json` |
| JSON copy dir | `JSON_COPY_DIR` | `./data/json-copies` |
| Environment | `APP_ENVIRONMENT` | `production` |
| reCAPTCHA test mode, keys, verify URL | `RECAPTCHA_TEST_MODE`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_VERIFY_URL` | `false`, none, none, Google URL |
| Organizer credentials, recipients | `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` | none (secret / required) |
| Organizer HTTPS-only | `ORGANIZER_HTTPS_ONLY` | `true` |
| CORS origin | `APP_CORS_ALLOWED_ORIGIN` | empty |
| Rate limits, body limit | `RATE_LIMIT_FORM_CONFIG`, `RATE_LIMIT_REGISTRATIONS`, `RATE_LIMIT_EXPORT`, `MAX_BODY_BYTES` | 60, 10, 10, 16384 |

## 13. Deployment, health (NFR-02, NFR-04, ES-09, SB-11)

- `02_output/docker-compose.yml`: `postgres` (volume `pgdata`, health `pg_isready`), `mailpit` (UI/API on `127.0.0.1:8025`), `backend` (built from the jar, volume `json-copies:/data/json-copies`, health `wget /actuator/health/readiness`), `frontend` (nginx on `127.0.0.1:8080`, health check on `/`, depends on a healthy backend). Secrets from the repository `.env` (`--env-file ../.env`); local sets `APP_ENVIRONMENT=local`, `RECAPTCHA_TEST_MODE=true`, `ORGANIZER_HTTPS_ONLY=false`.
- Backend exposes `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` (database included); no other actuator endpoints; details hidden.
- Data survives `docker compose down` / `up` because database and JSON copies are on named volumes (NFR-02).

## 14. Test design hooks (for phase 3)

- Backend acceptance tests: `@SpringBootTest` + MockMvc through the HTTP API, PostgreSQL and Mailpit via Testcontainers (`environments.md` test), test mode on, high rate limits; database checked with SQL through the documented schema, JSON copy through the file contract, emails through the Mailpit HTTP API. AC-003-02/03 start the application with alternative configuration files. DoD-P05: the production verifier against a local stub `siteverify` endpoint, accepted and rejected token.
- Frontend acceptance tests: Vitest + Testing Library on `App` with a stubbed `fetch`, through the UI contract.
- End-to-end tests: Playwright against `docker compose` (one test per E2E scenario), checking PostgreSQL via `docker compose exec postgres psql`, JSON copies via `docker compose exec backend`, Mailpit API and the export.

## Decisions

D-10 (AC-008-04 → 403), D-11 (AC-005-03 → 500), D-12 (fields of the other registration type are rejected).

## Traceability

| AC / SR / AR | Section |
|---|---|
| AC-001-01, AC-002-01 | 4.2 (step 7), 5 |
| AC-001-02, AC-002-02 | 4.2 (step 2) |
| AC-001-03 | 4.2 (step 2, trimming), 5.1 |
| AC-001-04, AC-002-03 | 4.2 (step 2, email) |
| AC-001-05, AC-002-04 | 4.2 (step 2, options), 3 |
| AC-001-06, AC-002-05 | 4.2 (step 2, consents), 3 |
| AC-001-07, AC-002-06 | 4.2 (step 4), 7 |
| AC-001-08, AC-002-07 | 4.1, 11.3, 11.4 |
| AC-003-01 | 4.1, 3 |
| AC-003-02 | 3.1, 3.4 |
| AC-003-03 | 3.3 |
| AC-004-01 | 11.7 |
| AC-004-02 | 11.7 |
| AC-004-03 | 11.6 |
| AC-004-04 | 11.7, 4.4 |
| AC-005-01 | 5.1, 5.2 |
| AC-005-02 | 5.3, 5.4 |
| AC-005-03 | 5.3, 4.4 |
| AC-005-04 | 4.2 (Unicode), 5.4 |
| AC-006-01 | 6.2 |
| AC-006-02 | 6.2 |
| AC-006-03 | 6.1, 4.2 (step 6) |
| AC-007-01 | 6.3 |
| AC-007-02 | 6.3, 5.4 |
| AC-008-01 | 4.3, 8 |
| AC-008-02 | 4.3.2 |
| AC-008-03 | 8 |
| AC-008-04 | 4.3.1 |
| SR-01 | 4.2 (step 4), 7.1 |
| SR-02 | 7.2, 7.3 |
| SR-03 | 9.1, 9.2 |
| SR-04 | 4.2 (options) |
| SR-05 | 6.2, 4.2 (email) |
| SR-06 | 4.3.1 |
| SR-07 | 5.4, 6.3, 8 |
| SR-08 | 9.9 |
| SB-01 | 4.2 (step 2) |
| SB-02 | 4.3.2, 9.5 |
| SB-03 | 4.3.2, 12 |
| SB-04 | 9.9, 1 |
| SB-05 | 9.6, 8 |
| SB-06 | 9.1 |
| SB-07 | 4.4, 9.7 |
| SB-08, SB-09 | 9.10 |
| SB-10 | 9.3 |
| SB-11 | 9.8, 13 |
| SB-12, SB-13, SB-14 | 10 |
| NFR-01 | 4.2 (Unicode), 5.4, 8 |
| NFR-02 | 5.4, 13 |
| NFR-03 | 11.6, 11.7 |
| NFR-04 | 13 |
| AR-01 | 1, 11.1 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 5.3 |
| AR-06 | 5.1 |
| AR-07 | 4.1, 11.8 |
