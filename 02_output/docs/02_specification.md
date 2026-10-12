# Specification

> Written in: phase 2 · Source: `01_acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Contracts in `docs/02_contracts/`: `openapi.yaml` (REST), `conference-options.schema.json` (configuration), `registration-copy.schema.json` (JSON copy), `database.md` (SQL), `emails.md` (SMTP), `anti-automation.md` (reCAPTCHA), `ui-form.md` (UI). Versions are only in `tech-stack.md`.

## S1. Components

| Component | Folder | Runs as | Talks to |
|---|---|---|---|
| backend | `02_output/backend` | Spring Boot 4 (Java 21) jar in an `eclipse-temurin` JRE image, user `app` (uid 1000) | PostgreSQL (JPA), SMTP, reCAPTCHA verify URL, JSON copy volume |
| frontend | `02_output/frontend` | React SPA built by Vite; static files in an `nginx` image, non-root, port 8080 | backend only through `/api` (AR-01) |
| database | `postgres` image | named volume `db-data` | |
| mail catcher | `axllent/mailpit` (local and tests only) | SMTP 1025, API 8025 | |

`02_output/docker-compose.yml` (started with `02_output/scripts/compose.sh`, which exports only the needed keys from `.env`, D-22) runs all four locally on `127.0.0.1` (frontend `127.0.0.1:8080`, Mailpit UI `127.0.0.1:8025`). The frontend nginx forwards `/api/` to the backend, so the browser uses one origin locally, as the external reverse proxy does in production (`environments.md`).

## S2. Backend architecture (AR-02, AR-03)

Declared style: **layered with ports**. Root package `si.konferenca.registration`.

| Package | Holds | May depend on |
|---|---|---|
| `domain` | registration model (`Registration`, `RegistrationType`, `Category`, `ConferenceOption`, `OptionCatalog`, `Consent`), validation rules and normalisation (BR-01..BR-05) | Java SE only (no Spring, JPA, servlet, mail, POI) |
| `application` | use cases (`RegistrationService`, `ExportService`), port interfaces (`RegistrationStore`, `RegistrationCopyWriter`, `Notifier`, `CaptchaVerifier`, `WorkbookWriter`) and `FormSettings` (public form settings handed to `web`) | `domain`, Spring transaction annotations |
| `web` | REST controllers, request/response DTOs, error mapping, rate-limit and size-limit filters | `application`, `domain` |
| `infrastructure.persistence` | JPA entities, Spring Data repositories, `RegistrationStore` adapter | `application`, `domain` |
| `infrastructure.jsoncopy` | `RegistrationCopyWriter` adapter (file system) | `application`, `domain` |
| `infrastructure.mail` | `Notifier` adapter (Spring Mail) | `application`, `domain` |
| `infrastructure.captcha` | `CaptchaVerifier` adapters (live, test mode) | `application`, `domain` |
| `infrastructure.export` | `WorkbookWriter` adapter (Apache POI) | `application`, `domain` |
| `config` | Spring configuration, `@ConfigurationProperties`, security, option catalog loading, startup guards | any package |

Why: the business rules sit in a framework-free core that unit tests reach directly; each external system (database, file, SMTP, Google, Excel) is behind a port, so acceptance tests can replace one at the boundary (for example a failing copy writer for AC-005-03) without touching the core.

ArchUnit rules (phase 3 acceptance level, `architecture` tests): the dependency table above as a layered architecture; `domain` has no dependency on `org.springframework..`, `jakarta.persistence..`, `jakarta.servlet..`, `jakarta.mail..`, `org.apache.poi..`; infrastructure packages do not depend on each other or on `web`; no package cycles (slices `si.konferenca.registration.(*)..` and `si.konferenca.registration.infrastructure.(*)..`, AR-03).

## S3. REST API (AR-01)

As in `openapi.yaml`:

| Endpoint | Access | Behaviour |
|---|---|---|
| `GET /api/options` | public, rate limited | active options with `registrationTypes` (AC-003-01) |
| `GET /api/settings` | public, rate limited | conference name, consent id and wording, reCAPTCHA mode and site key |
| `POST /api/registrations` | public, rate limited, size limited | S4 validation → S6 storage → S7 emails → `201` |
| `GET /api/organizer/export` | organizer (S9) | Excel workbook (S9) |
| `/actuator/health/liveness`, `/actuator/health/readiness` | public, not routed by the proxy | S12 |

Every other path under `/api` answers 404; error bodies are `application/problem+json` with a fixed title per status and no stack trace, exception message or class name (ES-07, SB-07). Status mapping: validation and token → 400 with `fieldErrors`; duplicate email → 409 with `fieldErrors[email]`; malformed JSON → 400 without `fieldErrors` details beyond `body`; body above limit → 413; wrong content type → 415; rate limit → 429 with `Retry-After`; storage failure → 500.

## S4. Validation (BR-01..BR-05, SB-01, SR-04)

Done in `domain` on the server for every request, whatever the client did (SB-01). The frontend repeats the field checks for usability only (NFR-03).

1. Trim every text field (BR-02); `null` and blank become missing.
2. `type` must be `EXTERNAL` or `STUDENT`.
3. Required per type (BR-01): both: `firstName`, `lastName`, `email`; EXTERNAL: `organization`; STUDENT: `studyInstitution`, `studyProgramme`, `studentId`. Fields of the other type must be absent or blank → field error `Not allowed for this registration type.`. Message for missing: `This field is required.`
4. Length after trimming: text ≤ 200, email ≤ 254 (D-18) → `Must be at most N characters.` Control characters (U+0000–U+001F, U+007F) are rejected → `Contains characters that are not allowed.`, except tab, CR and LF inside text fields other than email (AC-006-03; user text never reaches a mail header, S7). Any other Unicode is accepted unchanged (BR-03, NFR-01).
5. Email (BR-03): `^[^\s@]+@[^\s@]+\.[^\s@]+$` → `Enter a valid email address.`
6. Options (BR-04, SR-04, D-10, D-13): each id must exist in the configured catalog, be active, and be open to the type; no id twice. Otherwise one field error on `optionIds`: `Unknown, inactive or unavailable option: <id>` or `Option selected more than once: <id>`. The id is echoed only if it matches the identifier pattern; otherwise `Unknown option.`
7. Consent (BR-05): `consentGiven` must be `true` → `Consent is required.`
8. All field errors are collected and returned together (400). The token is verified (S8) only when 1–7 pass, so invalid requests cost no Google call.
9. Duplicate email (D-14): `lower(trim(email))` already stored → 409, field error `This email address is already registered.` Checked before insert and enforced by the unique index (a concurrent duplicate also gives 409).

## S5. Conference options (AR-04, US-003, BR-01)

- Mechanism: a JSON file validated against `conference-options.schema.json`, loaded once at startup from `OPTIONS_LOCATION` (Spring resource: `file:` or `classpath:`). Change = edit the file and restart (D-20). No code or participant field depends on it.
- Startup validation (AC-003-03): schema violations (unknown category, bad id, missing name) and duplicate ids stop the startup with a message naming the option index and id. Implemented by the `config` loader; the failure message never contains other configuration values.
- `registrationTypes` absent = both types (D-10).
- Default `classpath:conference-options.json` is allowed only in local and test; the `prod` profile requires `OPTIONS_LOCATION` to start with `file:` (startup guard).
- The consent id and wording also come from this file (D-11).

## S6. Storage and JSON copy (US-005, BR-07, AR-05, AR-06, ES-08)

- Schema and write order: `database.md`. Migration `V1__registration.sql` via Flyway; Hibernate `ddl-auto=validate`.
- `RegistrationService.register` is one transaction: insert (flush) → write JSON copy (temp file + atomic move to `<JSON_COPY_DIR>/<id>.json`, UTF-8, content per `registration-copy.schema.json`) → commit. A copy failure throws and rolls back (AC-005-03); a commit failure deletes the copy (AC-005-04). The response is sent only after both (AR-05).
- `JSON_COPY_DIR` must exist and be writable at startup (readiness fails otherwise). Files are created with owner-only permissions (`rw-------`).
- Persistence across container recreation (NFR-02): named volumes `db-data` and `registration-copies`.

## S7. Emails (US-006, US-007, SR-05, SR-07, D-12)

As in `emails.md`. Sent synchronously after the commit by `Notifier`; each send is wrapped so a failure is logged (id and kind only) and swallowed (AC-006-04). SMTP connection and read timeouts 5 s. No email for a rejected registration (AC-006-05), because emails happen only after a successful commit.

## S8. Anti-automation (SR-01, SR-02, AR-07)

As in `anti-automation.md`. `CaptchaVerifier` has two adapters selected by `RECAPTCHA_TEST_MODE`: live (Spring `RestClient` to `RECAPTCHA_VERIFY_URL`, 5 s timeouts, client IP from the request after the trusted-proxy rules of S10) and test (`test-mode-pass` only). Startup guards: test mode off + empty keys → stop; `prod` profile + test mode on → stop.

## S9. Organizer access and export (US-008, BR-08, SB-02, SB-03, SR-06, SR-07)

- Spring Security, HTTP Basic, one in-memory user from `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD` with role `ORGANIZER`; the password is BCrypt-hashed at startup and the plain value is not kept in any bean (SB-03). Empty username or password stops the startup. Sessions: stateless; CSRF off (no cookies, no browser session).
- Only `/api/organizer/**` requires the role; every other path is `permitAll` as listed in S3 (SB-02).
- SR-06: with `ORGANIZER_REQUIRE_HTTPS=true` (default) a request to `/api/organizer/**` that is not secure (scheme after `X-Forwarded-Proto` from a trusted proxy) and does not come from a loopback address gets 403 before authentication, so the credentials are never checked over plain HTTP. Local compose sets it to `false` (plain HTTP on 127.0.0.1 only).
- Failed organizer logins count against the export rate limit (SB-06).
- Workbook (Apache POI `XSSFWorkbook`): sheet `Registrations`; header row `Registration ID`, `Type`, `Submitted at`, `First name`, `Last name`, `Email`, `Organization / institution`, `Study institution`, `Study programme`, `Student ID`, `Workshops`, `Events`, `Meals`, `Other activities`, `Consent`, `Consent given at`; one row per registration ordered by submission time; options as display names joined by `; `; timestamps ISO-8601 UTC. Every value is written as a string cell (never a formula), so text starting with `=`, `+`, `-`, `@` stays text (AC-008-05, SB-05). Only these columns are written (SR-07). `Content-Disposition: attachment; filename="registrations.xlsx"`, `Cache-Control: no-store`.

## S10. Cross-cutting security (SB, SR)

| Control | Design |
|---|---|
| Rate limits (SB-06, SR-03) | Servlet filter, fixed window per client IP, in memory: `POST /api/registrations` `RATE_LIMIT_REGISTRATIONS_PER_MINUTE` (default 10); `GET /api/options` and `/api/settings` `RATE_LIMIT_READS_PER_MINUTE` (default 120); `/api/organizer/**` incl. failed logins `RATE_LIMIT_ORGANIZER_PER_MINUTE` (default 10) → 429 (D-19). Client IP: `server.forward-headers-strategy=native` with Tomcat's default internal-proxy ranges, so `X-Forwarded-For` counts only when it comes from the reverse proxy. |
| Request size (SR-03) | Filter rejects bodies above `MAX_REQUEST_BYTES` (default 16384) → 413, by `Content-Length` and by counting streamed bytes. |
| Security headers (SB-10) | Backend: `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`. Frontend nginx: CSP `default-src 'self'; script-src 'self' https://www.google.com/recaptcha/ https://www.gstatic.com/recaptcha/; frame-src https://www.google.com/recaptcha/ https://recaptcha.google.com/recaptcha/; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`, plus the same nosniff, framing and referrer headers. |
| CORS | Off unless `CORS_ALLOWED_ORIGINS` lists origins (local only); never `*`. |
| Output encoding (SB-05) | JSON by Jackson; React escapes text; emails plain text; Excel string cells; SQL via JPA bound parameters only. |
| Logs (ES-07, SB-07) | Log registration id, status and kind only; never names, emails, tokens, credentials or request bodies. Spring's request logging and SQL parameter logging stay off. |
| TLS (SB-04) | Terminated by the external reverse proxy in production; SMTP uses STARTTLS when `MAIL_TLS=true`; reCAPTCHA over HTTPS. Local runs on 127.0.0.1 only. |
| Least privilege (SB-11) | Both images run as non-root users; compose drops all capabilities and sets `no-new-privileges`; ports bound to 127.0.0.1. |
| Dependencies and source (SB-08, SB-09) | Dependency-Check, `npm audit`, Semgrep, gitleaks, SpotBugs, PMD in phase 6. |
| Secrets (ES-01, ES-02, SB-03) | Only from environment variables; no default value for any secret in code or properties. |

## S11. Configuration (ES-01)

| Variable | Default | Allowed default in | Notes |
|---|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/registration` | local, test | `prod` profile has no default |
| `DB_USERNAME` | `registration` | local, test | `prod` profile has no default |
| `POSTGRES_PASSWORD` | none (secret) | — | |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS` | `localhost`, `1025`, `false` | local, test | `prod` profile: no default for host and port, `MAIL_TLS` default `true` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | empty (secret) | — | production only |
| `MAIL_AUTH` | `false` (`prod` profile: `true`) | all | SMTP authentication with `SMTP_USERNAME`/`SMTP_PASSWORD` |
| `MAIL_FROM` | `registration@konferenca.local` | all | |
| `CONFERENCE_NAME` | `Conference` | all | |
| `OPTIONS_LOCATION` | `classpath:conference-options.json` | local, test | S5 |
| `JSON_COPY_DIR` | `/data/registrations` | all | persistent volume |
| `RECAPTCHA_TEST_MODE` | `false` | all | `true` only in test and local |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | empty (secret) | — | required unless test mode |
| `RECAPTCHA_VERIFY_URL` | Google siteverify | all | |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | none (secret) | — | |
| `ORGANIZER_EMAILS` | none | — | comma separated |
| `ORGANIZER_REQUIRE_HTTPS` | `true` | all | `false` only locally |
| `CORS_ALLOWED_ORIGINS` | empty | all | |
| `RATE_LIMIT_*`, `MAX_REQUEST_BYTES` | as in S10 | all | |
| `SPRING_PROFILES_ACTIVE` | none | — | `prod` in production |

## S12. Health and operations (ES-09, NFR-04)

Actuator exposes only `health` with liveness and readiness groups; readiness includes the database and the JSON copy directory check. Container health checks: backend `wget -qO- http://127.0.0.1:8080/actuator/health/readiness`; frontend `wget -qO- http://127.0.0.1:8080/healthz` (nginx static `ok`). Compose starts the backend after PostgreSQL is healthy and the frontend after the backend is healthy.

## S13. Frontend (US-001, US-002, US-004, NFR-03, AR-01, AR-07)

React function components, no router. `App` loads `/api/settings` and `/api/options`, then renders `RegistrationForm` (behaviour in `ui-form.md`) and, after a 201, `Confirmation`. A small `api.ts` wraps `fetch` for the three endpoints and maps 400/409 `fieldErrors`. Client validation mirrors S4 steps 1, 3, 5 and 7 and the token; the backend stays authoritative. No secret and no backend URL in the bundle: relative `/api` paths only (AR-01, AR-07). Text is rendered through React (no `dangerouslySetInnerHTML`).

## S14. Personal data (SB-12, SB-13, SB-14)

Collected: exactly the fields of BR-01 plus selected options and consent with timestamp, as in `security-requirements.md`; no IP address or token is stored (the IP is used in memory only for rate limiting and the Google verify call). Retention (D-15): until 12 months after the conference, deleted manually by the organizer (release notes). Consent (D-11): explicit, unchecked by default, wording and timestamp stored.

## S15. Test approach (summary; detail in phase 3)

| Level | Tool | Covers |
|---|---|---|
| acceptance (backend) | JUnit 6, Spring Boot test on a random port, `RestClient`, Testcontainers PostgreSQL and Mailpit | API, storage, JSON copy, emails, export, security, startup guards |
| architecture | ArchUnit | S2 rules, AR-03 |
| acceptance (frontend) | Vitest + Testing Library with a mocked `fetch` | form, client validation, error display, confirmation |
| e2e | Playwright against the compose stack | full flow incl. NFR-01 |

## Decisions in this phase

D-16 (Redocly CLI for contract validation), D-17 (English UI), D-18 (field length limits), D-19 (rate-limit defaults), D-20 (option changes need a restart).

## Traceability

| AC / SR / AR | Section |
|---|---|
| AC-001-01, AC-002-01 | S13, S5, `ui-form.md` |
| AC-001-02, AC-002-02 | S13, S5, `ui-form.md` |
| AC-001-03, AC-002-03 | S3, S4, S6 |
| AC-001-04, AC-001-05, AC-002-04 | S4 (1, 3) |
| AC-001-06, AC-002-05 | S4 (5) |
| AC-001-07 | S4 (4), S6 |
| AC-001-08, AC-001-09, AC-001-10, AC-001-11, AC-002-06 | S4 (6) |
| AC-001-12, AC-002-07 | S4 (7) |
| AC-001-13, AC-002-08 | S8, `anti-automation.md` |
| AC-001-14 | S4 (9), `database.md` |
| AC-001-15, AC-001-16, AC-002-09 | S13, `ui-form.md` |
| AC-003-01 | S3, S5 |
| AC-003-02 | S5 |
| AC-003-03 | S5 |
| AC-004-01, AC-004-02, AC-004-03 | S13, `ui-form.md` |
| AC-005-01, AC-005-02 | S6, `database.md`, `registration-copy.schema.json` |
| AC-005-03, AC-005-04 | S6 |
| AC-006-01, AC-006-02, AC-006-03, AC-006-04, AC-006-05 | S7, `emails.md` |
| AC-007-01, AC-007-02, AC-007-03 | S7, `emails.md` |
| AC-008-01, AC-008-02, AC-008-05, AC-008-06 | S9 |
| AC-008-03, AC-008-04 | S9 |
| AR-01 | S1, S3, S13 |
| AR-02 | S2 |
| AR-03 | S2 |
| AR-04 | S5 |
| AR-05 | S6 |
| AR-06 | S6 |
| AR-07 | S8, S13 |
| SR-01 | S8 |
| SR-02 | S8, S11 |
| SR-03 | S10 |
| SR-04 | S4 (6) |
| SR-05 | S7 |
| SR-06 | S9 |
| SR-07 | S7, S9 |
| SB-01 | S4 |
| SB-02 | S9 |
| SB-03 | S9, S10 |
| SB-04 | S10 |
| SB-05 | S9, S10 |
| SB-06 | S10 |
| SB-07 | S3, S10 |
| SB-08, SB-09 | S10 (phase 6 scans) |
| SB-10 | S10 |
| SB-11 | S1, S10 |
| SB-12, SB-13, SB-14 | S14 |
| NFR-01 | S4 (4), S7, S9, S15 (e2e) |
| NFR-02 | S6 |
| NFR-03 | S4, S13 |
| NFR-04 | S12 |
