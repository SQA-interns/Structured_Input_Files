# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Contracts and constraints only; the classes emerge in the TDD cycles of phase 4. Contracts in `docs/02_contracts/`:

| Contract | Interface (architecture.md) |
|---|---|
| `openapi.json` | options, registration, export (REST) |
| `database.sql` | registration storage (SQL via JPA, Flyway) |
| `registration-copy.schema.json` | JSON copy (file) |
| `conference-config.schema.json` | conference options and consents (file) |
| `email.md` | emails (SMTP) |
| `excel-export.md` | export workbook layout |
| `ui.md` | registration form (UI), anti-automation widget |
| `configuration.md` | environment settings, health endpoints |

## Components and interfaces

| AC | Component | Interface the acceptance test uses |
|---|---|---|
| AC-001-01..07, AC-002-01..06 | backend | `POST /api/registrations` (`openapi.json`); "nothing stored" read from `database.sql` tables and the JSON copy directory; "no email" read from the mail catcher |
| AC-001-08, AC-002-07 | frontend | `ui.md`, with `GET /api/form-config` answered by a test double of `openapi.json` |
| AC-003-01, AC-003-02 | backend | `GET /api/form-config` with `conference-config.schema.json` files given through `CONFERENCE_CONFIG_FILE` |
| AC-003-03 | backend | application start with an invalid configuration file (`conference-config.schema.json`, startup error) |
| AC-004-01..04 | frontend | `ui.md`, with `POST /api/registrations` answered by a test double (201, 400, 500, network error) |
| AC-005-01, AC-005-04 | backend | `POST /api/registrations`, then `database.sql` tables |
| AC-005-02, AC-005-03 | backend | `POST /api/registrations`, then `registration-copy.schema.json` files in `JSON_COPY_DIR` (made unwritable for AC-005-03) |
| AC-006-01..03, AC-007-01..02 | backend | `POST /api/registrations`, then the SMTP catcher (Mailpit API) per `email.md`; AC-006-03 with an unreachable SMTP port |
| AC-008-01..04 | backend | `GET /api/registrations/export` with HTTP Basic (`openapi.json`, `excel-export.md`) |
| E2E-01..05 | all | browser against the local stack (`docker compose`), Mailpit API, database and JSON copy read inside the containers |

## Backend architecture (AR-02)

Declared structure: a hexagonal-style layering under the root package `si.konferenca.registration`.

| Package | Holds | May depend on (inside the root package) |
|---|---|---|
| `..domain..` | registration, option, consent and conference-configuration model and the business rules (BR-01..05): validation, trimming, option availability | nothing outside `domain`; no Spring, Jakarta Persistence, servlet or mail types |
| `..application..` | use cases (register, list for export, form configuration) and the ports they need (store, JSON copy, notifier, captcha verifier) | `domain` |
| `..web..` | REST controllers, request/response mapping, error mapping, security configuration and filters (rate limit, size limit, HTTPS-only organizer access) | `application`, `domain` |
| `..infrastructure..` | port implementations: JPA persistence, JSON copy file writer, mail, reCAPTCHA client, Excel writer, configuration loader | `application`, `domain` |
| root | `RegistrationApplication` only | any |

Reason: the rules that must not regress (validation, option availability, storage before notification) live in `domain` and `application` and are testable without Spring or a database; every external system sits behind a port in `infrastructure`, so a test can replace it, and `web` cannot bypass the use cases to reach storage. ArchUnit rules in the test suite check exactly this table: the layer dependencies above, `web` and `infrastructure` not depending on each other, `domain` free of framework packages, and no cycles between the slices `si.konferenca.registration.(*)..` (AR-03).

Transaction order (AR-05, BR-07, OQ-03): within one database transaction the registration row is written, then the JSON copy file; a copy failure rolls back the transaction (and deletes a partly written file) and the API answers 500. Emails are sent from an after-commit listener, asynchronously, so storage always precedes notification (scope priority 1) and a mail failure cannot change the response.

## Options configuration mechanism (AR-04)

A JSON file outside the code, named by `CONFERENCE_CONFIG_FILE` (`conference-config.schema.json`), holds the conference name, the consents (OQ-02: shipped file has the mandatory consent `privacy` with the given wording) and the options with id, name, category, active flag and `availableTo` (OQ-01, default both types). It is read and validated once at startup; any violation stops the start (AC-003-03). A change of options is a new file plus a restart (AC-003-02). The participant fields are fixed in code and the file has no way to change them (BR-01). The shipped file is mounted read-only into the backend container by `docker-compose.yml`.

## Security and quality controls

| ID | Control or target |
|---|---|
| SB-01 | all validation in `domain` on the backend; the frontend checks are convenience only; unknown JSON properties rejected |
| SB-02 | Spring Security: `/api/registrations/export` requires the organizer role; everything else under `/api` is public; every other path denied |
| SB-03 | organizer password hashed with BCrypt at startup, plain value discarded; secrets only from environment (`configuration.md`), no defaults in code |
| SB-04 | production TLS at the external nginx (environments.md); organizer access requires HTTPS (SR-06); SMTP STARTTLS on by default (`MAIL_TLS`) |
| SB-05 | JPA with bound parameters only; React escapes output; emails plain text; Excel string cells only |
| SB-06 | per-client rate limits on all `/api` endpoints, including failed logins on the export (SR-03) |
| SB-07 | generic error bodies (`ErrorResponse`), no stack traces (`server.error.include-*=never`); logs carry the registration id only |
| SB-08 | Dependency-Check and `npm audit` in phase 0 and 6 |
| SB-09 | SpotBugs, PMD, Semgrep, gitleaks in phase 6 |
| SB-10 | backend: Spring Security headers plus `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`; frontend nginx: CSP allowing self and Google reCAPTCHA only, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer` |
| SB-11 | containers run as non-root users (backend `USER` in the image, nginx unprivileged on port 8080) |
| SB-12 | only the BR-01 fields, options and consents are stored; fields of the other type are ignored |
| SB-13 | purposes and retention as in `security-requirements.md`; retention instruction (OQ-06) in the release notes |
| SB-14 | consent checkboxes never preselected (`ui.md`); consent time stored |
| SR-01 | backend verifies every token (`app.recaptcha.*`); production path calls the verify URL; test mode only by configuration |
| SR-02 | `APP_ENVIRONMENT` defaults to `production`; production start fails with test mode on or empty keys |
| SR-03 | rate limits and `MAX_REQUEST_BYTES` (`configuration.md`), 429 and 413 responses |
| SR-04 | option ids checked against the active configured set and the type's availability; duplicates rejected (OQ-04) |
| SR-05 | `email.md`: plain text, fixed headers, validated addresses |
| SR-06 | `ORGANIZER_HTTPS_ONLY` default true: non-HTTPS, non-loopback export requests refused with 403 before authentication reaches data (G-3 of the requirements check, D-09) |
| SR-07 | export columns and organizer email limited to registration data (`excel-export.md`, `email.md`) |
| SR-08 | the external production proxy and the frontend nginx set `proxy_set_header Host $proxy_host` (never `$http_host`); release notes |
| NFR-01 | UTF-8 end to end (JSON, PostgreSQL `UTF8`, mail `charset=UTF-8`, POI strings); E2E-04 |
| NFR-02 | named volumes for PostgreSQL data and JSON copies; runtime demonstration in phase 6 |
| NFR-03 | client-side validation and backend field errors shown per field (`ui.md`) |
| NFR-04 | actuator liveness and readiness; compose health checks use them |
| AR-01 | frontend uses relative `/api` URLs only; nginx proxies `/api` to the backend |
| AR-03 | ArchUnit slice cycle check |
| AR-06 | Flyway migrations produce `database.sql`; `spring.jpa.hibernate.ddl-auto=validate` |
| AR-07 | the frontend gets the site key from `/api/form-config` at run time; no key in the bundle |

## Traceability

| AC / SR / SB / NFR / AR | Section or contract |
|---|---|
| AC-001-01..07, AC-002-01..06 | `openapi.json` `createRegistration`, `database.sql`, `registration-copy.schema.json` |
| AC-001-08, AC-002-07 | `ui.md` Elements, `openapi.json` `getFormConfig` |
| AC-003-01..03 | `conference-config.schema.json`, `openapi.json` `getFormConfig`, AR-04 section |
| AC-004-01..04 | `ui.md` Validation and errors, Confirmation |
| AC-005-01..04 | `database.sql`, `registration-copy.schema.json`, transaction order |
| AC-006-01..03, AC-007-01..02 | `email.md` |
| AC-008-01..04 | `openapi.json` `exportRegistrations`, `excel-export.md` |
| SB-01..14, SR-01..08, NFR-01..04 | Security and quality controls |
| AR-01..07 | Backend architecture, Options configuration mechanism, Security and quality controls |

Choices a user would notice, recorded as pending review: D-08 (English UI text), D-09 (403 for organizer access over plain HTTP, rate and size limit defaults, length limits).
