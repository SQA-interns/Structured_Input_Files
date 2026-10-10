# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

## 1. System overview

Two components (`architecture.md`), deployed as containers:

| Component | Folder | Role | Runs as |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot REST API under `/api`: form configuration, registration, export; PostgreSQL, JSON copies, emails, reCAPTCHA verification | `eclipse-temurin` JRE image, user `app` (uid 10001) |
| frontend | `02_output/frontend` | React single-page form (external and student), confirmation | static files in `nginx`, unprivileged user, port 8080 |

Local stack: `02_output/docker-compose.yml` with `postgres`, `mailpit`, `backend`, `frontend`; ports bound to 127.0.0.1 only (frontend 8081, backend 8080, Mailpit UI/API 8025). The frontend nginx forwards `/api/` to the backend, so the browser talks to one origin (AR-01). In production an external nginx terminates TLS, serves `/` from the frontend and forwards `/api` to the backend (`environments.md`).

## 2. Backend architecture (AR-02, AR-03)

Declared architecture: **layered with ports**. Root package `si.konferenca.registration`.

| Package | Contains | May depend on |
|---|---|---|
| `..domain` | Pure model and rules: `RegistrationType`, `OptionCategory`, `ConferenceOption`, `Consent`, `OptionCatalog`, `RegistrationRequest` validation (`RegistrationValidator`, `FieldError`), `AcceptedRegistration` | Java SE only (no Spring, no Jakarta Persistence, no other application package) |
| `..application` | Use cases: `RegistrationService`, `ExportService`, `FormConfigService`; ports (`RegistrationStore`, `JsonCopyStore`, `Notifier`, `CaptchaVerifier`, `WorkbookWriter`) | `domain`; Spring stereotypes and transactions |
| `..api` | REST controllers, request/response DTOs, `ApiExceptionHandler` | `application`, `domain` |
| `..persistence` | JPA entities, Spring Data repositories, `JpaRegistrationStore`, `FileJsonCopyStore` | `application`, `domain` |
| `..infrastructure` | `MailNotifier`, `GoogleRecaptchaVerifier`, `TestModeCaptchaVerifier`, `PoiWorkbookWriter`, `OptionCatalogLoader` | `application`, `domain` |
| `..config` | Spring Security, rate limiting and body size filters, typed properties (`AppProperties`), startup guards | every package (composition root) |

Reason: the domain rules (BR-01..BR-05) are tested without Spring; adapters for the database, file system, SMTP, reCAPTCHA and Excel are replaceable behind ports, which keeps the storage-before-notification rule (AR-05) in one service. ArchUnit rules (test `ArchitectureTest`) check exactly the table above: allowed dependencies per layer, `domain` free of Spring/Jakarta Persistence, `api` not using `persistence`/`infrastructure`, adapters not using `api`, and no slice cycles under `si.konferenca.registration.(*)..` (AR-03).

## 3. Configuration (ES-01, AR-04, AR-07)

All settings are Spring properties bound to `AppProperties` (`app.*`) and set through environment variables in containers. Secrets have no default.

| Property (env var) | Meaning | Default |
|---|---|---|
| `app.environment` (`APP_ENVIRONMENT`) | `production`, `local` or `test` | `production` |
| `spring.datasource.url` / `username` / `password` (`SPRING_DATASOURCE_*`, password from `POSTGRES_PASSWORD`) | database | none (local/test set them) |
| `spring.mail.host` / `port` / `username` / `password`, `spring.mail.properties.mail.smtp.starttls.enable` | SMTP | none |
| `app.mail.sender` (`APP_MAIL_SENDER`) | From address | `registration@konferenca.si` |
| `app.conference-name` (`APP_CONFERENCE_NAME`) | shown in emails and the form | `Conference` |
| `app.organizer.emails` (`ORGANIZER_EMAILS`) | notification recipients, comma separated | none (required) |
| `app.organizer.username` / `password` (`ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`) | export credentials | none (required, password ≥ 16 characters) |
| `app.organizer.https-only` (`APP_ORGANIZER_HTTPS_ONLY`) | SR-06 | `true` |
| `app.options.location` (`APP_OPTIONS_LOCATION`) | options and consents file (Spring resource) | `classpath:conference-options.json`, refused in production |
| `app.json-copy.directory` (`APP_JSON_COPY_DIRECTORY`) | JSON copy directory (persistent volume) | `/data/json-copies` |
| `app.recaptcha.test-mode` (`APP_RECAPTCHA_TEST_MODE`) | deterministic test mode | `false` |
| `app.recaptcha.site-key` / `secret-key` (`RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`) | Google keys | none |
| `app.recaptcha.verify-url` (`APP_RECAPTCHA_VERIFY_URL`) | verification endpoint | `https://www.google.com/recaptcha/api/siteverify` |
| `app.cors.allowed-origin` (`APP_CORS_ALLOWED_ORIGIN`) | CORS origin, local only | empty (no CORS) |
| `app.rate-limit.registration.max-requests` / `window` | SR-03 | `10` per `PT10M` |
| `app.rate-limit.export.max-requests` / `window` | SR-03 | `20` per `PT10M` |
| `app.request.max-body-bytes` | SR-03 | `16384` |

Startup guards (`StartupGuard`, fail start with a message naming the setting, never its value):
- `app.environment=production` and (`app.recaptcha.test-mode=true` or empty site/secret key) → refuse (SR-02).
- `app.environment=production` and options location is the classpath default → refuse (`environments.md`: default allowed local/test only).
- `app.environment=production` and `app.organizer.https-only=false` → refuse (`environments.md`: may be off only on local).
- `app.environment=production` and `app.cors.allowed-origin` not empty → refuse (CORS local only).
- organizer username, password (< 16 characters) or emails missing → refuse (ES-01).
- Options file invalid (contract `02_contracts/conference-options.schema.json`): duplicate id, unknown category/audience, empty name, consent without text → refuse (AC-003-03).

### Conference options (AR-04, US-003)

Options and consents live in one JSON file (`02_contracts/conference-options.schema.json`), loaded once at startup into an immutable `OptionCatalog`. Changing programme = edit the file + restart; no code change, and the participant fields are fixed in code (BR-01). The file is mounted read-only into the backend container (`02_output/config/conference-options.json` locally). Each option: `id` (stable, `^[a-z0-9][a-z0-9-]{0,63}$`), `name`, `category` (`WORKSHOP`, `EVENT`, `MEAL`, `OTHER`), `active`, `audience` (subset of `EXTERNAL`, `STUDENT`, default both, D-08). Each consent: `id`, `text`, `mandatory`.

## 4. REST API (contract `02_contracts/openapi.yaml`)

| Operation | Access | Behaviour | ACs |
|---|---|---|---|
| `GET /api/form-config` | public | conference name, active options (id, name, category, audience), consents (id, text, mandatory), `recaptcha.siteKey`, `recaptcha.testMode` | AC-003-01, AC-003-02, AC-001-01, AC-002-01, AC-002-04 |
| `POST /api/registrations` | public, rate limited, size limited, reCAPTCHA | validate → verify reCAPTCHA → duplicate check → store (DB + JSON copy, one transaction) → `201` → emails | AC-001-02..13, AC-002-02..04, AC-005-*, AC-006-*, AC-007-* |
| `GET /api/export/registrations.xlsx` | organizer (HTTP Basic), HTTPS-only, rate limited | `.xlsx` workbook | AC-008-* |
| `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | public, backend port only (not routed via `/api`) | health and readiness (ES-09, NFR-04) | — |

Error body (all errors, SB-07): `{status, error, message, fieldErrors[{field, code, message}]}`; `error` codes: `VALIDATION_FAILED` (400), `CAPTCHA_FAILED` (400, field `captchaToken`), `DUPLICATE_EMAIL` (409, field `email`), `UNAUTHORIZED` (401), `HTTPS_REQUIRED` (403), `PAYLOAD_TOO_LARGE` (413), `UNSUPPORTED_MEDIA_TYPE` (415), `TOO_MANY_REQUESTS` (429), `INTERNAL_ERROR` (500). Messages are fixed texts; no exception text, class name or stack trace is returned.

### Registration processing (BR-02..BR-07, AR-05, D-10..D-14)

1. **Validation** (`RegistrationValidator`, domain, SB-01): trim all strings (BR-02); required fields per type (BR-01: external = firstName, lastName, email, organization; student = firstName, lastName, email, studyInstitution, studyProgramme, studentId); fields of the other type must be absent or empty (`NOT_ALLOWED`); max length 200 (email 254); reject any Unicode control character (`Character.getType == CONTROL`, D-14, SR-05); email: one `@`, non-empty local part ≤ 64 of `[A-Za-z0-9!#$%&'*+/=?^_`{\|}~.-]` without leading/trailing/double dot, domain of dot-separated labels `[A-Za-z0-9-]` (no leading/trailing hyphen) with at least one dot and a TLD of ≥ 2 letters (BR-03); option ids must exist, be active and include the type in their audience, no duplicates (BR-04, SR-04, D-08, D-11); every mandatory consent id given, unknown consent ids rejected (BR-05); `captchaToken` non-empty. Field codes: `REQUIRED`, `TOO_LONG`, `INVALID_CHARACTERS`, `INVALID_EMAIL`, `NOT_ALLOWED`, `UNKNOWN_OPTION`, `OPTION_NOT_AVAILABLE`, `DUPLICATE_OPTION`, `CONSENT_REQUIRED`, `UNKNOWN_CONSENT`. All errors are collected and returned together (400).
2. **reCAPTCHA** (SR-01): `CaptchaVerifier.verify(token, clientIp)`. Production: `GoogleRecaptchaVerifier` POSTs `secret`, `response`, `remoteip` (form encoded) to `app.recaptcha.verify-url` with 5 s timeouts and accepts only `{"success": true}` (contract `02_contracts/recaptcha-siteverify.openapi.yaml`); any other answer, HTTP error or timeout rejects. Test mode (`TestModeCaptchaVerifier`, only when `app.recaptcha.test-mode=true`): accepts exactly the token `test-pass`. Rejected → 400 `CAPTCHA_FAILED`.
3. **Duplicate email** (D-12): `email_normalized = lower(trim(email))` (root locale) checked, and enforced by a unique index; a concurrent duplicate that hits the index maps to 409 as well.
4. **Storage** (AR-05, BR-07): in one `@Transactional` method, insert the registration rows and flush, then `JsonCopyStore.write(reference, bytes)` (write to a temp file in the same directory, `fsync`, atomic move to `registration-<reference>.json`). If writing fails, the exception rolls the transaction back (AC-005-02). If the commit fails after the file is written, a transaction synchronisation deletes the file (AC-005-03). Only then is `201` built. The JSON bytes are produced once and reused for the file and the organizer attachment (AC-007-01).
5. **Notifications** (D-10): after commit, `Notifier.sendParticipantConfirmation` and `Notifier.sendOrganizerNotification`, each in its own try/catch; failures are logged with the reference only (no personal data, ES-07). SMTP connect/read timeouts 5 s.

Response `201`: `{reference, submittedAt, type, firstName, lastName, options[{id, name, category}]}` (used by the confirmation, BR-06).

Reference: random UUID (v4) string. Submission time: UTC instant, millisecond precision.

## 5. Persistence (AR-06, ES-08, contract `02_contracts/database-schema.sql`)

Flyway migrations in `backend/src/main/resources/db/migration` (`V1__create_registration_tables.sql`); `spring.jpa.hibernate.ddl-auto=validate`. Tables: `registration` (internal `id`, `reference` unique, `type`, the participant fields, `email_normalized` unique, `submitted_at`), `registration_option` (option id, name snapshot, category, position), `registration_consent` (consent id, text snapshot, `given_at`). Text columns are `varchar` in a UTF-8 database (NFR-01). Option names are snapshotted so the export stays correct after the configuration changes.

JSON copy (contract `02_contracts/registration-copy.schema.json`): UTF-8, one file per registration in `app.json-copy.directory`, never overwritten (`CREATE_NEW` semantics on the final name). Both database and directory are on named volumes (`pgdata`, `jsoncopies`) that survive `docker compose down`/`up` (NFR-02).

## 6. Emails (SR-05, SR-07, contract `02_contracts/emails.yaml`)

`MailNotifier` with `JavaMailSender` and `MimeMessageHelper`, UTF-8, `text/plain` only (markup stays literal, AC-006-02). Header values are fixed or from configuration: From = `app.mail.sender`, Subject = `<conference name> – registration confirmed (<reference>)` / `<conference name> – new registration <reference>`; the only participant value in any header is the participant's address as recipient of the confirmation, already validated without control characters. Organizer email: To = all `app.organizer.emails`, body lists the registration data only (labels + values, options by category, consents with time), attachment `registration-<reference>.json` (`application/json`, the JSON copy bytes).

## 7. Export (US-008, BR-08, SR-06, SR-07)

`ExportService` reads all registrations ordered by submission time; `PoiWorkbookWriter` writes one sheet `Registrations` with header row and columns: Reference, Submitted at (UTC ISO-8601), Type, First name, Last name, Email, Organization / institution, Study institution, Study programme, Student ID, Workshops, Events, Meals, Other activities, Consents. Every cell is a string cell (no formula evaluation of user input). Internal ids are never exported. Response: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, `Content-Disposition: attachment; filename="registrations.xlsx"`, `Cache-Control: no-store`.

## 8. Security (ASVS 5.0 Level 1, SB-01..SB-14, SR-01..SR-07)

- Spring Security filter chain: `/api/export/**` requires role `ORGANIZER` via HTTP Basic, everything else under `/api/**` and `/actuator/health/**` is `permitAll`; all other paths denied. Stateless (no session), CSRF disabled because no cookie/session authentication exists (Basic credentials are not sent automatically cross-site from a form); 401 responses carry the JSON error body and `WWW-Authenticate: Basic realm="organizer"`.
- Organizer password: hashed with BCrypt (strength 12) at startup into an in-memory user store; the plain value is not retained in the user store (SB-03).
- `HttpsOnlyFilter` before authentication: when `app.organizer.https-only=true`, a request to `/api/export/**` that is not secure (`request.isSecure()`, which honours `X-Forwarded-Proto` from trusted proxies through `server.forward-headers-strategy=native`) and whose remote address is not loopback is refused with 403 `HTTPS_REQUIRED` (SR-06, AC-008-03).
- `RateLimitFilter`: fixed window per client IP (`request.getRemoteAddr()` after forwarded-header processing) and bucket (`POST /api/registrations`, `/api/export/**`), in memory, limits from configuration → 429 with `Retry-After` (SR-03, SB-06). Single backend instance assumed (release notes).
- `RequestSizeFilter`: `Content-Length` above `app.request.max-body-bytes` → 413; bodies without length are read through a counting stream that fails with 413 when exceeded (SR-03).
- Headers (SB-10): `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'` on API responses, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx: CSP `default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; connect-src 'self'; img-src 'self' data:; style-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`, plus `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`.
- CORS: only when `app.cors.allowed-origin` is set (local development via Vite), `GET`/`POST`, no credentials.
- JPA with bound parameters only (SB-05); React escapes output (SB-05).
- Logging: no request bodies, no personal data, no secrets; errors logged with the reference or an error id (ES-07, SB-07). `server.error.include-*` = never.
- Containers run as non-root (SB-11); TLS outside localhost is provided by the external nginx (SB-04).
- Privacy (SB-12..SB-14): only the fields of BR-01 are collected; purposes and retention in `security-requirements.md` with D-13 (≤ 12 months after the conference, manual deletion); consents unchecked by default and stored with time (SB-14, D-09).
- Scanning (SB-08, SB-09): Dependency-Check and `npm audit` (dependencies), SpotBugs, PMD and Semgrep (static analysis), Gitleaks (secrets) in phase 6.

## 9. Frontend (contract `02_contracts/registration-ui.yaml`)

`App` loads `GET /api/form-config`, shows a type choice (radio: external participant / student), then `RegistrationForm` with the type's fields, options grouped by category (checkboxes, filtered by audience), consents (unchecked checkboxes), the `Captcha` component and a submit button. Client validation mirrors the backend rules for required, length, control characters, email format and mandatory consents (NFR-03); errors render in an element `id="<field>-error"` referenced by `aria-describedby`. On `201` the form is replaced by `Confirmation` (reference, name, options) (BR-06). On 400/409 the backend `fieldErrors` are shown next to their fields; on other errors a general message without internal details is shown and values stay (AC-004-02). The `Captcha` component: in test mode renders a checkbox "I am not a robot (test mode)" yielding the token `test-pass`; otherwise loads `https://www.google.com/recaptcha/api.js` and renders the v2 widget with the site key from `/api/form-config` (AR-07); the widget is reset after every rejected submission. All calls use relative `/api/...` URLs (AR-01).

## 10. Health and operations (ES-09, NFR-04)

Actuator exposes only `health` with liveness and readiness groups (`management.endpoint.health.probes.enabled=true`, details never shown). Readiness includes the database. Compose health checks: backend `wget -qO- http://127.0.0.1:8080/actuator/health/readiness`, frontend `wget -qO- http://127.0.0.1:8080/`, postgres `pg_isready`; `depends_on: condition: service_healthy`.

## 11. Commands (ES-05)

| Component | Build | Test | Check | Run |
|---|---|---|---|---|
| backend | `./mvnw -B -DskipTests package` | `./mvnw -B verify` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` (with local settings) or `docker compose up backend` |
| frontend | `npm run build` | `npm test` (unit/component/acceptance), `npm run test:e2e` (against the local stack) | `npm run check` | `npm run dev` or `docker compose up frontend` |

## 12. Tests (planned, detailed in `03_test-strategy.md`)

- Backend acceptance (`src/test/java/.../acceptance/`): `@SpringBootTest(webEnvironment = RANDOM_PORT)` over HTTP, Testcontainers PostgreSQL, Mailpit container (Testcontainers `GenericContainer`, image from `tech-stack.md`) read through its HTTP API, a JDK `HttpServer` as the mocked reCAPTCHA endpoint (production code path, DoD-P05), temporary JSON copy directory.
- Frontend acceptance (`tests/acceptance/`): Vitest + Testing Library through the rendered UI with a mocked `fetch`.
- End-to-end (`e2e/`): Playwright against the compose stack (frontend 8081, Mailpit 8025), test-mode reCAPTCHA.
- Architecture: ArchUnit `ArchitectureTest` (AR-02, AR-03, AR-06 no JDBC DDL outside migrations is not checkable by ArchUnit; checked by review).
- Unit: phase 5.

## Traceability

| AC / SR / SB / NFR / AR | Section |
|---|---|
| AC-001-01, AC-002-01 | 4 (`GET /api/form-config`), 9 |
| AC-001-02, AC-002-02 | 4 processing 1–4, 5, 7 |
| AC-001-03, AC-002-03 | 4 processing 1 (`REQUIRED`) |
| AC-001-04 | 4 processing 1 (trim) |
| AC-001-05 | 4 processing 1 (`INVALID_EMAIL`) |
| AC-001-06 | 5, 6, 7 (UTF-8 throughout), NFR-01 |
| AC-001-07 | 4 processing 1 (option checks) |
| AC-001-08 | 4 processing 1 (`CONSENT_REQUIRED`) |
| AC-001-09 | 4 processing 2 |
| AC-001-10 | 4 processing 3 |
| AC-001-11 | 4 processing 1 (length, control characters) |
| AC-001-12 | 9 |
| AC-001-13 | 8 (`RateLimitFilter`, `RequestSizeFilter`) |
| AC-002-04 | 3 (audience), 4 processing 1, 9 |
| AC-003-01 | 3 (options), 4 |
| AC-003-02 | 3 (options file + restart) |
| AC-003-03 | 3 (startup guards) |
| AC-004-01, AC-004-02 | 9 |
| AC-005-01 | 4 processing 4, 5 |
| AC-005-02, AC-005-03 | 4 processing 4 |
| AC-006-01, AC-006-02 | 6 |
| AC-006-03 | 4 processing 5 |
| AC-007-01, AC-007-02 | 6 |
| AC-008-01, AC-008-05 | 7 |
| AC-008-02 | 8 (Spring Security) |
| AC-008-03 | 8 (`HttpsOnlyFilter`) |
| AC-008-04 | 8 (`RateLimitFilter`) |
| SR-01 | 4 processing 2, 9 |
| SR-02 | 3 (startup guards) |
| SR-03 | 8 |
| SR-04 | 4 processing 1 |
| SR-05 | 4 processing 1, 6 |
| SR-06 | 8 |
| SR-07 | 6, 7 |
| SB-01 | 4 processing 1 |
| SB-02 | 8 |
| SB-03 | 8 (BCrypt), 3 (secrets from environment) |
| SB-04 | 1, 8 (external TLS) |
| SB-05 | 8 |
| SB-06 | 8 |
| SB-07 | 4 (error body), 8 (logging) |
| SB-08, SB-09 | 8 (scanning) |
| SB-10 | 8 (headers) |
| SB-11 | 1, 8 |
| SB-12, SB-13, SB-14 | 8 (privacy), D-09, D-13 |
| NFR-01 | 5, 6, 7, 9; e2e test |
| NFR-02 | 5 (named volumes); runtime demonstration |
| NFR-03 | 9 |
| NFR-04 | 10 |
| AR-01 | 1, 9 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 (options) |
| AR-05 | 4 processing 4 |
| AR-06 | 5 |
| AR-07 | 3, 9 |
