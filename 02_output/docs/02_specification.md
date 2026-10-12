# Specification

> Written in: phase 2 · Source: `project/01_requirements/acceptance-criteria.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` · Agent: writes

Contracts referenced below live in `docs/02_contracts/`. Versions are only in `tech-stack.md`.

## 1. System overview

Two components (`architecture.md`): **backend** (Spring Boot API, root package `si.konferenca.registration`) and **frontend** (React single page built with Vite, served by nginx). PostgreSQL stores registrations; a named volume holds the raw JSON copies; an SMTP server (Mailpit locally) receives the emails; Google reCAPTCHA v2 protects the registration (test mode locally and in tests).

```
browser ──/──► frontend (nginx: static files, /api proxy) ──/api──► backend ──► PostgreSQL
                                                                    ├──► JSON copy volume
                                                                    ├──► SMTP
                                                                    └──► reCAPTCHA siteverify (production)
```

The frontend calls only `/api/**` (AR-01). In production an external nginx terminates TLS, serves `/` from the frontend and forwards `/api` to the backend (`environments.md`).

## 2. Backend architecture (AR-02, AR-03)

Declared style: **layered core with ports for side effects**. Packages under `si.konferenca.registration`:

| Package | Responsibility | May depend on (application packages) |
|---|---|---|
| `domain` | registration types, option categories, conference catalog, submission, accepted registration, validation rules (pure Java, no Spring, no JPA) | none |
| `catalog` | loads and validates the conference configuration file at startup | `domain` |
| `service` | registration use case (validate, verify reCAPTCHA, store, write copy, notify); declares the ports `CaptchaVerifier`, `JsonCopyStore`, `RegistrationNotifier` | `domain`, `persistence` |
| `persistence` | JPA entities, Spring Data repository, mapping to `domain` | `domain` |
| `storage` | JSON copy files (implements `JsonCopyStore`) | `domain`, `service` |
| `mail` | participant and organizer emails (implements `RegistrationNotifier`) | `domain`, `service` |
| `captcha` | reCAPTCHA verification, test mode (implements `CaptchaVerifier`) | `domain`, `service` |
| `export` | Excel workbook of all registrations | `domain`, `persistence` |
| `security` | Spring Security, organizer Basic auth, HTTPS-only filter, rate limit and body-size filters, production start guard | `domain` |
| `web` | REST controllers, request/response DTOs, error mapping | `domain`, `catalog`, `service`, `export` |
| root | `RegistrationApplication` and `@ConfigurationProperties` (`RegistrationProperties`) | any |

ArchUnit rules (written in phase 5, checked in phase 6, DoD-04) check exactly this table:

1. `domain` depends on no other application package and on no `org.springframework..`, `jakarta.persistence..` class.
2. Each package depends only on the packages listed in its row (plus the root package for `RegistrationProperties`, JDK, and third-party libraries).
3. No package depends on `web` (except `web` itself).
4. `service` does not depend on `storage`, `mail` or `captcha` (ports invert the dependency).
5. No cycles between the packages above (slices `si.konferenca.registration.(*)..`, AR-03).

Justification: the use case must enforce the storage-before-response order (AR-05) and storage-before-email order (OQ-03) in one place (`service`); the side effects are behind ports so the order is testable and the adapters cannot call back into the web layer. Persistence is used directly by `service` because there is only one store and a port would add no checkable value.

## 3. Conference configuration (US-003, AR-04, BR-04, BR-05, OQ-01, OQ-02)

- One JSON file, schema `02_contracts/conference-config.schema.json`, location from the setting `registration.conference-config` (env `REGISTRATION_CONFERENCE_CONFIG`; default `classpath:conference.json`, the shipped configuration, local and test only; the `prod` profile has no default).
- Contents: `options[]` (`id`, `name`, `category` ∈ `WORKSHOP`, `EVENT`, `MEAL`, `OTHER`, `active` default `true`, `availableTo` ⊆ {`EXTERNAL`, `STUDENT`} default both) and `consents[]` (`id`, `text`, `mandatory`).
- Loaded once at startup. The backend **refuses to start** (exception during context start, non-zero exit) when: the file is missing or not valid JSON; an option or consent id is duplicated, missing or does not match `^[a-z0-9][a-z0-9-]{0,63}$`; a category or registration type is unknown; a name or consent text is missing or blank; `availableTo` is empty (AC-003-03).
- Changing the programme = editing this file and restarting; no code change and the participant fields are not configurable (BR-01, AC-003-02).
- Shipped configuration (`backend/src/main/resources/conference.json`): two workshops (one external-only), two events, two meals, one other activity, one inactive workshop, and the mandatory consent `privacy` with the OQ-02 wording.

## 4. REST API (contract `02_contracts/openapi.json`)

| Method and path | Access | Purpose | Responses |
|---|---|---|---|
| `GET /api/form-config` | public, rate limited | conference name, reCAPTCHA site key and test-mode flag, consents, **active** options with `id`, `name`, `category`, `availableTo` (AC-003-01, AC-003-02) | 200, 429 |
| `POST /api/registrations` | public, reCAPTCHA, rate limited, body ≤ 16 KiB | submit a registration | 201 `{id, registeredAt}`; 400 field errors; 413; 415; 429; 500 |
| `GET /api/organizer/registrations.xlsx` | organizer (HTTP Basic), HTTPS-only, rate limited | Excel export (US-008) | 200 workbook; 401; 403 (plain HTTP from non-localhost); 429 |

Health and readiness: `GET /actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` on the backend port, not routed by the proxies (NFR-04, ES-09). Nothing else of the actuator is exposed.

### 4.1 Registration request

JSON object with `type` (`EXTERNAL` | `STUDENT`), `firstName`, `lastName`, `email`, `organization` (external), `studyInstitution`, `studyProgramme`, `studentId` (student), `optionIds` (array of strings, may be empty), `consents` (array of given consent ids), `recaptchaToken`. Unknown properties are rejected (400).

### 4.2 Validation (SB-01, BR-01..05, SR-01, SR-04; order of evaluation does not matter, all field errors are returned together)

| Field key | Rule | Message |
|---|---|---|
| `type` | required, `EXTERNAL` or `STUDENT` | "Choose a registration type." |
| `firstName`, `lastName` | required after trimming; ≤ 100 characters | "This field is required." / "Use at most 100 characters." |
| `email` | required after trimming; ≤ 254 characters; matches `^[^\s@]+@[^\s@]+\.[^\s@]+$` | "Enter a valid email address." |
| `organization` | EXTERNAL: required, ≤ 200; STUDENT: must be absent or empty | required / "This field does not apply to the registration type." |
| `studyInstitution`, `studyProgramme` | STUDENT: required, ≤ 200; EXTERNAL: absent or empty | as above |
| `studentId` | STUDENT: required, ≤ 50; EXTERNAL: absent or empty | as above |
| `optionIds` | each id is a configured **active** option available to the type; no id twice; ≤ 100 entries | "Select only available options, each at most once." |
| `consents.<id>` | every mandatory consent id is in `consents`; unknown ids rejected under `consents` | "This consent is required." |
| `recaptchaToken` | present and accepted by the verifier | "Confirm that you are not a robot." |

Trimming: leading and trailing whitespace (`String.strip()`) is removed from every text field before validation and storage (BR-02, AC-001-03). Inner characters are kept as entered, including line breaks and markup (they are encoded on output, SR-05). Unicode is accepted (BR-03).

reCAPTCHA is verified only when the token is present; the result is added to the other field errors. Rejected submissions create no row, no copy and no email (AC-001-07, AC-002-06).

### 4.3 Error model (SB-07, ES-07)

Every error body is `{"message": "<user text>", "fieldErrors": {"<field key>": "<message>"}}` (`fieldErrors` empty when not a field problem). No stack traces, exception names, SQL or paths are returned.

| Situation | Status | Message |
|---|---|---|
| Field errors | 400 | "Please correct the marked fields." |
| Malformed JSON, unknown property, wrong type | 400 | "The request could not be read." |
| Body over 16 KiB | 413 | "The request is too large." |
| Content type not JSON | 415 | "The request could not be read." |
| Rate limit exceeded | 429 (+ `Retry-After`) | "Too many requests. Please try again later." |
| Organizer credentials missing or wrong | 401 (+ `WWW-Authenticate: Basic realm="organizer"`) | "Authentication required." |
| Organizer request over plain HTTP from a non-loopback client while HTTPS-only is on | 403 | "HTTPS is required." |
| Storage or copy failure, any unexpected error | 500 | "The registration could not be completed. Please try again later." |

## 5. Registration flow (BR-06, BR-07, AR-05, OQ-03, US-005..007)

1. Validate (4.2); verify reCAPTCHA; on any error answer 400.
2. In one database transaction: insert the registration, its options (id, name and category as configured at that moment) and consents (id, text, time given = registration time); flush; write the JSON copy `<id>.json` (write to a temporary file in the same directory, then atomic move). If writing the copy fails, the transaction rolls back and the API answers 500 (AC-005-03). If the commit fails after the copy was written, the copy is deleted.
3. After the commit: send the participant confirmation, then one organizer notification per configured organizer address (attachment = the bytes of the stored copy). Each send failure is logged as `Email <kind> failed for registration <id>` with no personal data, and the registration stays accepted (OQ-03, AC-006-03).
4. Answer 201 with `{id, registeredAt}`.

`id` is a random UUID; `registeredAt` is UTC with millisecond precision. Several registrations with the same email are allowed (OQ-05).

## 6. Persistence (contract `02_contracts/database.sql`, AR-06, ES-08)

Flyway migration `V1__registration_schema.sql` is the contract file verbatim: tables `registration`, `registration_option`, `registration_consent`, text columns `varchar` (UTF-8 database), times `timestamptz`. `spring.jpa.hibernate.ddl-auto=validate`; schema changes only through new migrations. Data lives on the `pgdata` named volume (NFR-02). No automatic deletion (OQ-06); release notes tell the operator to delete rows and copies at the latest 12 months after the conference.

## 7. JSON copy (contract `02_contracts/registration-copy.schema.json`)

UTF-8 file `<json-copy-dir>/<id>.json`, setting `registration.json-copy-dir` (env `REGISTRATION_JSON_COPY_DIR`, default `./data/json-copies`; containers `/data/json-copies` on the `jsoncopies` named volume). Contents: `id`, `type`, `registeredAt`, the type's own fields (trimmed), `options[]` (`id`, `name`, `category`), `consents[]` (`id`, `text`, `givenAt`). Non-ASCII characters are written as UTF-8, not escaped (NFR-01, AC-005-04). No internal fields (SR-07).

## 8. Emails (contract `02_contracts/emails.md`; SR-05, SR-07, US-006, US-007)

- Plain text (`text/plain; charset=UTF-8`) only; user input appears only in the body, never in a header. Subject lines contain only the configured conference name. The recipient header is built from the validated address alone (no display name). Markup is therefore literal text (AC-006-02).
- Participant confirmation: to the participant; names conference, participant and selected options.
- Organizer notification: one message per address in `ORGANIZER_EMAILS` (comma separated); body lists every submitted field, options and consents; attachment `registration-<id>.json` (`application/json`) equal to the stored copy (AC-007-02).
- Settings: `spring.mail.host`/`port` (env `SMTP_HOST`, `SMTP_PORT`, defaults Mailpit `localhost:1025` for local/test only), `SMTP_USERNAME`/`SMTP_PASSWORD`, `SMTP_STARTTLS` (default off locally), sender `registration.mail.from` (default `registration@konferenca.si`), conference name `registration.conference-name`; connection and read timeouts 5 s.

## 9. reCAPTCHA (SR-01, SR-02, DoD-P05)

- Production path: `POST` form `secret`, `response`, `remoteip` to `registration.recaptcha.verify-url` (default `https://www.google.com/recaptcha/api/siteverify`); accepted only when the answer is HTTP 200 with `"success": true`. Timeouts or errors count as rejected.
- Test mode (`registration.recaptcha.test-mode`, env `RECAPTCHA_TEST_MODE`, default **false**): no network call; exactly the token `test-mode-token` is accepted, every other token is rejected. The frontend shows a stand-in checkbox "I am not a robot (test mode)" instead of the Google widget and sends that token.
- Start guard (`prod` profile): the backend refuses to start when test mode is on, when `RECAPTCHA_SITE_KEY` or `RECAPTCHA_SECRET_KEY` is empty, or when organizer HTTPS-only is off.
- Site key is delivered to the frontend through `GET /api/form-config` (AR-07); the secret key never leaves the backend.

## 10. Export (US-008, BR-08, SR-06, SR-07)

- `GET /api/organizer/registrations.xlsx`, HTTP Basic against `ORGANIZER_USERNAME` / `ORGANIZER_PASSWORD` (no defaults; the password is held only as a BCrypt hash after startup, SB-03). Stateless, no session, no CSRF token needed (no cookies).
- HTTPS-only filter runs before authentication: when `registration.organizer.https-only` (env `ORGANIZER_HTTPS_ONLY`, default **true**) is on and the request is not secure and the client address is not loopback, answer 403 and do not evaluate credentials (AC-008-04). The client address and scheme come from `X-Forwarded-For` / `X-Forwarded-Proto` only when set by a trusted internal proxy (`server.forward-headers-strategy=native`, Tomcat's internal-proxy ranges).
- Workbook: one sheet "Registrations", header row then one row per registration ordered by registration time: Registration ID, Type, Registered at (UTC ISO-8601), First name, Last name, Email, Organization / institution, Study institution, Study programme, Student ID, Options (names joined by "; "), Consents (`<id> given <time>` joined by "; "). All cells are string cells (no formulas). `Content-Disposition: attachment; filename="registrations.xlsx"`.

## 11. Security controls

| ID | Implementation |
|---|---|
| SB-01 | server-side validation 4.2 for every input, unknown JSON properties rejected, frontend checks only for usability |
| SB-02 | only the export is non-public; Spring Security requires role `ORGANIZER` on `/api/organizer/**`, everything else under `/api` explicitly permitted, all other paths denied except health |
| SB-03 | BCrypt hash of the organizer password created at startup; secrets only from env (ES-01, ES-02) |
| SB-04 | TLS at the external nginx in production; plain HTTP only on 127.0.0.1 locally; SR-06 filter |
| SB-05 | JPA parameter binding only; React escapes output; plain-text emails; string cells in Excel |
| SB-06, SR-03 | per-client fixed-window rate limits: form config 120/min, registrations 20/min, organizer export 10/min (also limits failed logins); configurable; body limit 16 KiB on `/api/**` (also `client_max_body_size 16k` in nginx) |
| SB-07, ES-07 | error model 4.3; logs carry registration ids only, never personal data or secrets; `server.error.include-*=never` |
| SB-08 | Dependency-Check and `npm audit` (phase 0 and phase 6) |
| SB-09 | Semgrep and gitleaks (phase 6) |
| SB-10 | API: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store`; frontend nginx: CSP allowing self and Google reCAPTCHA hosts only, `frame-ancestors 'none'`, `nosniff`, `Referrer-Policy` |
| SB-11 | backend image runs as a non-root user; frontend nginx runs as user `nginx` on port 8080; PostgreSQL and Mailpit official images |
| SB-12, SB-13 | only the fields in `business-rules.md` are collected; purposes and retention as in `security-requirements.md` (OQ-06) |
| SB-14 | consent checkboxes are never preselected (UI contract); the backend requires each mandatory consent explicitly in `consents` and stores the time given |
| SR-01 | backend verification 9; frontend widget alone never accepted |
| SR-02 | test mode only by env, default off; `prod` start guard 9 |
| SR-04 | option ids checked against the active configured set and the registration type (4.2) |
| SR-05 | 8 |
| SR-06 | 10 |
| SR-07 | export and organizer email contain only the registration's fields, options, consents, id and time |
| SR-08 | frontend nginx sets `proxy_set_header Host` to the upstream name and overwrites `X-Forwarded-For` with `$remote_addr`; the production proxy setup in the release notes does the same |

CORS: no cross-origin access by default; `registration.cors.allowed-origins` (env `REGISTRATION_CORS_ALLOWED_ORIGINS`) may be set for local development only.

## 12. Frontend (contract `02_contracts/ui.md`; US-001, US-002, US-004, NFR-03)

- One page. On load it fetches `/api/form-config`. The participant chooses "External participant" or "Student"; the form then shows exactly that type's fields, the active options available to the type grouped into fieldsets Workshops, Events, Meals, Other activities (empty groups hidden), the consent checkboxes (unchecked), the reCAPTCHA (Google widget with the configured site key, or the test-mode stand-in) and the button "Register".
- Before sending, the same rules as 4.2 for required fields, lengths and email format are checked in the browser; on any error the errors are shown next to the fields and no request is sent (AC-004-03).
- 201: the form is replaced by the confirmation (participant's name, selected option names) (AC-004-01).
- 400 with `fieldErrors`: each message next to its field, entered values kept, reCAPTCHA reset (AC-004-02).
- Any other status or a network failure: general message "The registration could not be completed. Please try again later." without technical details; values kept (AC-004-04).
- Built as static files; served by nginx, which proxies `/api/` to the backend (AR-01). No secret in the bundle (AR-07).

## 13. Configuration summary (ES-01; `environments.md`)

| Setting | Env variable | Default (where allowed) |
|---|---|---|
| Database URL, user | `DATABASE_URL`, `DATABASE_USER` | `jdbc:postgresql://localhost:5432/registration`, `registration` (local/test; `prod` none) |
| Database password | `POSTGRES_PASSWORD` | none |
| SMTP host, port, STARTTLS, user, password | `SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS`, `SMTP_USERNAME`, `SMTP_PASSWORD` | `localhost`, `1025`, `false` (local/test; `prod` host none) |
| Sender, conference name | `REGISTRATION_MAIL_FROM`, `REGISTRATION_CONFERENCE_NAME` | `registration@konferenca.si`, `Conference` |
| Organizer recipients | `ORGANIZER_EMAILS` | none |
| Options source | `REGISTRATION_CONFERENCE_CONFIG` | `classpath:conference.json` (local/test; `prod` none) |
| JSON copy directory | `REGISTRATION_JSON_COPY_DIR` | `./data/json-copies` |
| reCAPTCHA test mode, keys, verify URL | `RECAPTCHA_TEST_MODE`, `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_VERIFY_URL` | `false`, empty, empty, Google URL |
| Organizer login, HTTPS-only | `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_HTTPS_ONLY` | none, none, `true` |
| CORS origins | `REGISTRATION_CORS_ALLOWED_ORIGINS` | empty |
| Rate limits, body limit | `REGISTRATION_RATE_LIMIT_FORM_CONFIG`, `REGISTRATION_RATE_LIMIT_REGISTRATIONS`, `REGISTRATION_RATE_LIMIT_EXPORT`, `REGISTRATION_MAX_BODY_BYTES` | 120 / 20 / 10 per minute, 16384 |

The variable names in this table are the configuration contract: `application.properties` maps each one explicitly (`property=${NAME:default}`, or `${NAME}` where no default is allowed), so the same names work as environment variables and as Spring properties (tests set them as properties). The `prod` profile (`SPRING_PROFILES_ACTIVE=prod`) removes the local defaults marked "local/test". `server.forward-headers-strategy=native` is always on.

## 14. Deployment and local stack (NFR-02, NFR-04, ES-09, SB-11)

`02_output/docker-compose.yml`: `postgres` (volume `pgdata`, `pg_isready` health check), `mailpit` (SMTP 1025, UI/API 8025), `backend` (image from `backend/Dockerfile`: `tech-stack.md` lists only the Temurin JRE image, so the jar is built on the host with `./mvnw package` and copied into the `eclipse-temurin` JRE image; non-root; volume `jsoncopies`; health check on `/actuator/health/readiness`), `frontend` (multi-stage: `node` image builds, `nginx` image serves; health check on `/`). Every port is published on `127.0.0.1` only: frontend 8081, backend 8080, Mailpit 8025, PostgreSQL 5432. Secrets come from the repository-root `.env` through `env_file`. Local stack sets `RECAPTCHA_TEST_MODE=true` and `ORGANIZER_HTTPS_ONLY=false` (allowed on local plain HTTP only).

## 15. Quality requirements

| ID | How it is met | How it is checked |
|---|---|---|
| NFR-01 | UTF-8 everywhere: PostgreSQL UTF8 database, JSON copy UTF-8 unescaped, emails `charset=UTF-8`, POI string cells, `<meta charset="UTF-8">` | AC-005-04, AC-008-03 tests, E2E-04 |
| NFR-02 | named volumes `pgdata`, `jsoncopies` | phase 6 runtime demonstration (`down` then `up`) |
| NFR-03 | client validation and server field errors next to fields (12) | AC-004-02, AC-004-03 component tests, E2E-03 |
| NFR-04 | actuator health with liveness and readiness groups; compose health checks | phase 6 runtime demonstration |
| DoD-P01..P04 | local stack, Mailpit API, export | E2E-01, E2E-02, E2E-05 and the phase 6 runtime demonstration |
| DoD-P05 | production reCAPTCHA path against a mocked siteverify endpoint, accepted and rejected token | backend acceptance tests for AC-001-07 / AC-002-06 |

## Traceability

| AC / SR / SB / AR / NFR | Section |
|---|---|
| AC-001-01, AC-002-01 | 4, 4.1, 5 |
| AC-001-02, AC-001-03, AC-001-04, AC-002-02, AC-002-03 | 4.2 |
| AC-001-05, AC-002-04 | 3, 4.2 |
| AC-001-06, AC-002-05 | 3, 4.2 |
| AC-001-07, AC-002-06 | 4.2, 9 |
| AC-001-08, AC-002-07 | 3, 12, contract `ui.md` |
| AC-003-01, AC-003-02, AC-003-03 | 3, 4 |
| AC-004-01 … AC-004-04 | 12, 4.3 |
| AC-005-01, AC-005-02, AC-005-03, AC-005-04 | 5, 6, 7 |
| AC-006-01, AC-006-02, AC-006-03 | 5, 8 |
| AC-007-01, AC-007-02 | 5, 8 |
| AC-008-01 … AC-008-04 | 10 |
| SR-01 … SR-08 | 11 (and 8, 9, 10) |
| SB-01 … SB-14 | 11 |
| AR-01 | 1, 12 |
| AR-02, AR-03 | 2 |
| AR-04 | 3 |
| AR-05 | 5 |
| AR-06 | 6 |
| AR-07 | 9, 12 |
| NFR-01 … NFR-04 | 15 |
| ES-01, ES-08, ES-09 | 13, 6, 14 |
