# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

## 1. System overview

Two components (`architecture.md`): `backend` (Spring Boot REST API, Java 21) and `frontend` (React single page built by Vite, served by nginx). Locally `02_output/docker-compose.yml` runs backend, frontend, PostgreSQL and Mailpit; in production an external nginx terminates TLS, serves `/` from the frontend container and forwards `/api` to the backend container.

The frontend calls only `/api/**` with relative URLs (AR-01). In the local stack the frontend's own nginx forwards `/api` to the backend, so the browser sees one origin and CORS is not needed; the Vite dev server proxies `/api` the same way.

## 2. Backend architecture (AR-02 declaration)

Ports and adapters around a framework-light domain. Root package `si.konferenca.registration`.

| Package | Responsibility | May depend on (application packages) |
|---|---|---|
| `domain` | Model (`RegistrationType`, `Registration`, `ConferenceOption`, `ConsentDefinition`, …), business rules (`RegistrationValidator`), use case (`RegistrationService`), ports (`RegistrationStore`, `JsonCopyStore`, `CaptchaVerifier`, `RegistrationNotifier`, `ConferenceCatalog`) | none |
| `api` | REST controllers, request/response DTOs, error mapping, request filters (rate and size limits, HTTPS-only organizer guard) | `domain` |
| `adapter.persistence` | JPA entities, Spring Data repositories, implementation of `RegistrationStore` | `domain` |
| `adapter.jsoncopy` | Raw JSON copy files, implementation of `JsonCopyStore` | `domain` |
| `adapter.mail` | Participant and organizer emails, implementation of `RegistrationNotifier` | `domain` |
| `adapter.captcha` | reCAPTCHA verification (test mode and production), implementation of `CaptchaVerifier` | `domain` |
| `adapter.export` | Excel workbook generation | `domain` |
| `adapter.catalog` | Loads and validates the conference configuration file, implementation of `ConferenceCatalog` | `domain` |
| `config` | Spring wiring, typed properties, security configuration, startup safety checks | all of the above |

Justification: the domain holds every business rule (BR-01 … BR-08) and can be tested without Spring MVC, JPA or SMTP; each external system (PostgreSQL, file system, SMTP, Google, configuration file) sits behind one port so the order "store, then notify" (AR-05, D-07) is expressed once in `RegistrationService`. Dependencies point inwards only.

ArchUnit rules (DoD-04), in `src/test/java/.../architecture/ArchitectureTest.java` (phase 5):

1. `domain` depends on no other application package and on no `org.springframework.web..`, `jakarta.persistence..`, `org.springframework.data..`, `jakarta.mail..`, `org.springframework.mail..`, `org.apache.poi..` class.
2. `api` depends only on `domain` among application packages.
3. Each `adapter.*` package depends only on `domain` among application packages (no adapter → adapter, no adapter → `api`, no adapter → `config`).
4. Only `config` may depend on `adapter.*` (besides the adapter itself).
5. No cycles between the slices `si.konferenca.registration.(*)..` (AR-03).
6. Flyway is the only schema source: `spring.jpa.hibernate.ddl-auto=validate` (AR-06, checked by configuration test).

## 3. Configuration (ES-01, AR-04)

Environment variables (Spring relaxed binding). "—" = no default; the application refuses to start without it.

| Setting | Variable | Default | Notes |
|---|---|---|---|
| Environment name | `APP_ENVIRONMENT` | `production` | `production`, `local` or `test`. Defaulting to `production` makes every production safety check apply unless an environment opts out. |
| Database URL, user, password | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | — | compose supplies them; tests use Testcontainers `@ServiceConnection`. Password from `POSTGRES_PASSWORD`. |
| SMTP host, port, TLS | `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `APP_MAIL_STARTTLS` | —, `25`, `true` | local: Mailpit, TLS off. Connection, read and write timeouts 5 s. |
| SMTP account | `SMTP_USERNAME`, `SMTP_PASSWORD` | empty | production only. |
| Sender address | `APP_MAIL_FROM` | `registration@konferenca.si` | |
| Organizer recipients | `ORGANIZER_EMAILS` | — | comma separated, each validated at startup. |
| Conference name | `APP_CONFERENCE_NAME` | `Conference` | shown in emails and on the page. |
| Conference configuration file | `APP_CONFERENCE_CONFIG_FILE` | classpath `conference-options.json` | production refuses the classpath default (`environments.md`: default allowed local/test only). |
| JSON copy directory | `APP_JSON_COPY_DIR` | `data/json-copies` | created at startup if missing; on a named volume in compose. |
| reCAPTCHA test mode | `APP_RECAPTCHA_TEST_MODE` | `false` | SR-02. |
| reCAPTCHA keys | `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty | required unless test mode. |
| reCAPTCHA verify URL | `APP_RECAPTCHA_VERIFY_URL` | `https://www.google.com/recaptcha/api/siteverify` | overridden only in tests (mock endpoint). |
| Organizer login | `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | — | password ≥ 16 characters, hashed with BCrypt at startup; the plain value is not kept. |
| Organizer HTTPS-only | `APP_ORGANIZER_HTTPS_ONLY` | `true` | may be `false` only when `APP_ENVIRONMENT` is not `production`. |
| CORS allowed origin | `APP_CORS_ALLOWED_ORIGIN` | empty (CORS off) | allowed only when `APP_ENVIRONMENT=local`. |
| Rate limits | `APP_RATE_LIMIT_REGISTRATION_PER_MINUTE`, `APP_RATE_LIMIT_FORM_CONFIG_PER_MINUTE`, `APP_RATE_LIMIT_EXPORT_PER_MINUTE` | `10`, `120`, `10` | per client address, fixed one-minute window. |
| Request-size limit | `APP_MAX_REQUEST_BYTES` | `16384` | request bodies on `/api/**`. |

Startup safety checks (`config.StartupSafetyChecks`), all fail the start with a message naming the setting (never its value): in `production`, test mode on, empty reCAPTCHA keys, HTTPS-only off, CORS origin set, or the classpath conference configuration (SR-02, environments.md); in every environment, empty organizer username/password, password shorter than 16 characters, empty or invalid organizer email list, invalid conference configuration (AC-003-03).

### Conference configuration (AR-04, US-003, D-05, D-06)

A JSON file validated against `02_contracts/conference-config.schema.json` at startup and held in memory; changing options means editing the file and restarting the backend. Participant fields are fixed in code (BR-01) and are not part of the file. Additional semantic checks: option and consent ids unique; at least one option category value from the enum; `availableTo` non-empty. Any violation stops the start (AC-003-03). The shipped file `src/main/resources/conference-options.json` contains sample options in every category and the consent `privacy` from D-06.

## 4. REST API (contract: `02_contracts/openapi.json`)

| Operation | Access | Behaviour |
|---|---|---|
| `GET /api/form-config` | public, rate limited | Conference name, captcha settings (`mode` `test` or `recaptcha`, `siteKey`), consents, active options (AC-003-01, AC-003-02). Inactive options are never returned. |
| `POST /api/registrations` | public, rate limited, size limited, reCAPTCHA | Validates, stores, writes the JSON copy, answers 201, then sends emails (sections 5–7). |
| `GET /api/export` | organizer (HTTP Basic), rate limited, HTTPS-only guard | Excel workbook of all registrations (section 8). |
| `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | public, not under `/api` (not forwarded by the production proxy) | NFR-04, ES-09. Details hidden. |

Every other path answers 401/403/404 without internal detail. Errors use one body shape: `{"message": "...", "fieldErrors": [{"field": "...", "message": "..."}]}`. Status codes: 400 validation or rejected captcha, 401 missing/wrong organizer credentials, 403 organizer credentials over plain HTTP (SR-06), 413 body too large, 415 wrong content type, 429 rate limit, 503 captcha verifier unreachable, 500 any other error (generic message, ES-07).

### Registration validation (domain, SB-01, BR-01 … BR-05)

Applied in this order; all field errors of one request are returned together.

1. Unknown JSON properties → 400 (`spring.jackson.deserialization.fail-on-unknown-properties=true`); `type` must be `EXTERNAL` or `STUDENT`.
2. Every string is trimmed (BR-02); required fields of the type must be non-empty after trimming; fields of the other type must be absent or empty (fixed fields, BR-01 — field error "not allowed for this registration type").
3. Lengths: names ≤ 100, email ≤ 254, organization / study institution / study programme ≤ 200, student ID ≤ 50. Any Unicode letter is allowed (BR-03, NFR-01).
4. Email: `^[^\s@]+@[^\s@]+\.[^\s@]+$` and parseable by `jakarta.mail.internet.InternetAddress(strict)`; this also excludes line breaks (SR-05).
5. Options: each id must exist, be active and be available to the type; duplicates are rejected (BR-04, SR-04, D-05, D-08). Field `optionIds`.
6. Consents: every mandatory consent id must be in `consentIds`; unknown ids are rejected. Field `consents.<id>` (BR-05).
7. Captcha: `captchaToken` must be present and verified (SR-01). Field `captchaToken`. Verification runs only when steps 1–6 passed, so invalid input costs no call to Google.

## 5. Storage (US-005, BR-07, AR-05, AR-06, ES-08)

`RegistrationService.register` performs, in one database transaction: insert the registration with its options and consents (flushed), then write the JSON copy; the transaction commits only after the copy is written. If the copy fails, the transaction rolls back and the API answers 500 (AC-005-03). If the commit fails after the copy was written, the copy is deleted. The 201 response is sent only after commit (AR-05).

Database (contract `02_contracts/database.md`, Flyway `V1__registration_schema.sql`): tables `registration`, `registration_option`, `registration_consent`; option name and category and the consent text are stored as they were at registration time, so later configuration changes do not alter stored registrations. `spring.jpa.hibernate.ddl-auto=validate`, `open-in-view=false`. All queries via Spring Data / JPA parameters (SB-05).

JSON copy (contract `02_contracts/registration-copy.schema.json`): one UTF-8 file `<registration-id>.json` in `APP_JSON_COPY_DIR`, written to a temporary file in the same directory and moved atomically. Content: the registration as accepted (trimmed values, options with name and category, consents with text and time); never the captcha token or any internal field (SR-07).

Registration ids are random UUIDs generated by the backend.

## 6. Participant email (US-006, SR-05, D-07)

Sent after commit on a background executor (so the response does not wait for SMTP), one plain-text UTF-8 message to the participant address (contract `02_contracts/email.md`). Subject contains only the configured conference name. User input appears only in the body; plain text means markup is never interpreted (SR-05); the recipient is the validated address. A send failure is logged with the registration id only and does not affect the accepted registration (AC-006-03, D-07).

## 7. Organizer notification (US-007, SR-07)

After commit, one plain-text message to every address in `ORGANIZER_EMAILS`, listing the registration data, with the JSON copy file attached as `registration-<id>.json` (`application/json`, the exact bytes of the stored copy, AC-007-02). Same failure handling as section 6.

## 8. Excel export (US-008, BR-08, SR-06, SR-07)

`GET /api/export` returns `registrations.xlsx` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment`) generated with Apache POI. Sheet `Registrations`, header row, one row per registration ordered by registration time. Columns: Registration ID, Registered at (UTC ISO-8601), Type, First name, Last name, Email, Organization / institution, Study institution, Study programme, Student ID, Workshops, Events, Meals, Other activities (option names, `; ` separated), Consents (`id at time`). All values written as string cells, so no cell is evaluated as a formula (SB-05).

Access (SB-02, SB-03, SR-06): Spring Security HTTP Basic, single in-memory organizer with the BCrypt hash of `ORGANIZER_PASSWORD`; stateless, no session, no CSRF token needed (no cookies). Before authentication a filter refuses (403, no body data) any `/api/export` request that is not secure, unless the client address is loopback or HTTPS-only is off. Tomcat's remote-IP handling (`server.forward-headers-strategy=native`) trusts `X-Forwarded-For`/`X-Forwarded-Proto` only from internal proxy addresses (Tomcat default), so the production proxy's HTTPS is recognised and a client cannot fake it directly.

## 9. Anti-automation (SR-01, SR-02, DoD-P05; contract `02_contracts/recaptcha.md`)

`CaptchaVerifier` has two implementations selected by `APP_RECAPTCHA_TEST_MODE`:

- Test mode (test and local only): token `TEST-PASS` is accepted, any other token rejected; no network call.
- Production: `POST` form `secret`, `response`, `remoteip` to the verify URL with a 5 s timeout; accepted only if the JSON answer has `"success": true`. An answer with `success: false` → 400; no answer, non-200 or unparsable → 503. Tested against a mocked endpoint (an in-test HTTP server), including a rejected token (DoD-P05).

The frontend renders the Google widget only in `recaptcha` mode, with the site key from `/api/form-config` (AR-07); in `test` mode it shows a note and sends `TEST-PASS`.

## 10. Rate and size limits (SR-03, SB-06)

`api.RequestLimitFilter` before Spring Security: per client address (after remote-IP resolution) and per endpoint group (form configuration, registration, export), a fixed one-minute window counter in memory with the configured limit; above it → 429 with `Retry-After`. Request bodies on `/api/**` above `APP_MAX_REQUEST_BYTES` → 413 (checked by `Content-Length` and by counting the stream). Old windows are evicted. The export limit also limits failed logins (PA-04 evidence).

## 11. Security headers, CORS, errors (SB-07, SB-10, ES-07)

- Backend responses: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`; HSTS on secure requests.
- Frontend nginx: CSP `default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; connect-src 'self'; img-src 'self' data:; style-src 'self'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`, plus `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: same-origin`; `server_tokens off`.
- CORS: none unless `APP_CORS_ALLOWED_ORIGIN` is set (local only), then exactly that origin for `/api/**`, `GET`/`POST`.
- A global exception handler maps every exception to the error shape without stack traces or class names; `server.error.include-*=never`. Logs contain registration ids, never names, emails or other personal data or secrets.

## 12. Frontend (UI contract: `02_contracts/ui.md`)

One page. On load it fetches `/api/form-config`; the participant picks the type (radio), fills the fixed fields of that type, picks options (checkboxes grouped by category; for a student only options available to students), ticks consents (never preselected, BR-05), solves the captcha and submits. Client-side validation (required, email format, mandatory consents) runs before any request (AC-004-03, NFR-03); server field errors are shown next to their fields (AC-004-02); a server or network failure shows a general error (AC-004-04); success replaces the form with the confirmation (AC-004-01, BR-06). Text is rendered by React (escaped, SB-05). No secrets in the bundle (AR-07).

## 13. Deployment, health, privacy

- Images: backend `eclipse-temurin` JRE base, runs as a non-root user; frontend built in `node` and served by unprivileged nginx on port 8080 inside the container (SB-11). Health checks: backend `/actuator/health/readiness`, frontend `/`, PostgreSQL `pg_isready` (NFR-04).
- Named volumes `pgdata` and `jsoncopies` keep data across container recreation (NFR-02).
- Local ports bound to `127.0.0.1` only: frontend 8081, backend 8080, Mailpit UI 8025.
- Production reverse proxy example `02_output/deploy/nginx-production.conf.example`: TLS, HTTP→HTTPS redirect, `/` → frontend, `/api/` → backend, `proxy_set_header Host` set to a fixed upstream name and `X-Forwarded-For $remote_addr` (SR-08), `client_max_body_size 16k`.
- Personal data (SB-12, SB-13): exactly the fields in `security-requirements.md`; purpose as listed there; retention per D-10 (operator deletes ≤ 12 months after the conference). Consent explicit, never preselected (SB-14).

## Traceability

| AC / SR / SB / NFR / AR | Section |
|---|---|
| AC-001-01, AC-002-01 | 4, 5 |
| AC-001-02, AC-002-02, AC-001-03 | 4 (validation 2) |
| AC-001-04, AC-002-03 | 4 (validation 4) |
| AC-001-05, AC-002-04 | 4 (validation 5), 3 |
| AC-001-06, AC-002-05 | 4 (validation 6) |
| AC-001-07, AC-002-06 | 9, 4 (validation 7) |
| AC-001-08, AC-002-07 | 12, `ui.md` |
| AC-003-01, AC-003-02 | 3, 4 |
| AC-003-03 | 3 (startup checks) |
| AC-004-01 … AC-004-04 | 12, `ui.md` |
| AC-005-01, AC-005-02, AC-005-03 | 5 |
| AC-005-04 | 4 (validation 3), 5 |
| AC-006-01 … AC-006-03 | 6, `email.md` |
| AC-007-01, AC-007-02 | 7, `email.md` |
| AC-008-01, AC-008-03 | 8 |
| AC-008-02, AC-008-04 | 8 |
| SR-01, SR-02 | 9, 3 |
| SR-03 | 10 |
| SR-04 | 4 (validation 5) |
| SR-05 | 6, 4 (validation 4) |
| SR-06 | 8 |
| SR-07 | 5, 7, 8 |
| SR-08 | 13 |
| SB-01 | 4 |
| SB-02, SB-03 | 8 |
| SB-04 | 13 (TLS at the proxy), 8 (HTTPS-only) |
| SB-05 | 5, 8, 12 |
| SB-06 | 10 |
| SB-07 | 11 |
| SB-08, SB-09 | scans in phase 0 and 6 (`tech-stack.md` tooling) |
| SB-10 | 11 |
| SB-11 | 13 |
| SB-12, SB-13, SB-14 | 13, 12, D-06, D-10 |
| NFR-01 | 4 (validation 3), 5, 6, 8; end-to-end test |
| NFR-02 | 13 |
| NFR-03 | 12 |
| NFR-04 | 4, 13 |
| AR-01 | 1, 12 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 5 |
| AR-06 | 2 (rule 6), 5 |
| AR-07 | 9, 12 |
