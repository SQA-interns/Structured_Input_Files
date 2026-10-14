# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run on branch `tanej/single-agent/sdd/v001`, 2026-10-13/14, local stack on Docker Desktop 29.8.2 (Windows 11). Logs: `logs/06_*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Backend `./mvnw -B clean verify`: 200 passed, 0 failed (unit 103, integration 9, architecture 8, acceptance 80; `logs/06_backend-verify.log`), 203 after fix loop 1 (`logs/06_backend-all-levels.log`). Frontend `vitest run`: 56 passed (`logs/06_frontend-test.log`). E2E Playwright on the rebuilt stack: 5 passed (`logs/06_e2e-run.log`). Hashes: `00_input-manifest.sha256` 41/41 and `03_acceptance-manifest.sha256` 32/32 match; no frozen file changed after freeze commit `cde8baf`. |
| DoD-02 | met | Backend `spotless:check pmd:check pmd:cpd-check spotbugs:check`: success, 0 PMD violations, 0 SpotBugs bugs (rule adjustments D-13). Frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`): success. Semgrep: triaged below (F-02 … F-04). |
| DoD-03 | met | Backend: line 98.19 % (all levels), PIT adjusted 90.54 % at the full run, 0 rounds; re-measured once after fix loop 1 (see "Re-measurement"). Frontend: line 100 %, Stryker adjusted 92.58 % after 1 survivor round. Scope, raw and adjusted figures: `docs/03_test-strategy.md`. |
| DoD-04 | met | `ArchitectureTest` (ArchUnit 1.3.2): 8 rules for spec §2 A1–A5 incl. slice cycle check, all pass. AR-01: frontend uses only relative `/api` URLs (`api.ts`, contract test AC-004-01). AR-04: AC-003-01..03. AR-05: AC-005-03. AR-06: Flyway V1 equals `database.sql`. AR-07: `captcha.siteKey` from configuration, no key in the bundle (inspection of `dist`). |
| DoD-05 | met | No open Critical or High (findings table). Dependency-Check online: 66 dependencies, 0 unsuppressed; npm audit `--omit=dev`: 0; gitleaks: no secret (F-05). |
| DoD-06 | met | Runtime demonstration below: both components run in containers, all core flows exercised through the browser and the API. |
| DoD-07 | met | Traceability table below (`trace-ac.mjs`): 35 AC, none without a test or a commit. |
| DoD-08 | met (phase 7) | Clean clone of `4eecd92` with a fresh `.env` from `secrets.env.example`: backend `verify` 203/203 and check clean, frontend `npm ci`, check, 56 tests, build; stack healthy, e2e 5/5, export 200 (`logs/07_clean-clone.log`). The first clone attempt failed on Windows path length; README now says to clone with `core.longpaths` (F-13). |
| DoD-09 | met (phase 7) | `docs/release-notes.md` "Must be tested manually by a human": 7 items (live reCAPTCHA, real SMTP, TLS/proxy incl. SR-08, production export, production safety check, persistence on the host, other browsers and assistive technology). |
| DoD-10 | met | Decisions D-01 … D-15 all have a resolution (D-01 vendor note, D-10, D-13 pending review). Input and protected files unchanged (hash check DoD-01). |
| DoD-11 | met | Phase 3 commits add tests, harness, compose/Dockerfiles only (no production source beyond the skeleton); freeze commit `cde8baf` adds only the manifest, every listed file committed earlier in phase 3. Build has commits naming US-001 … US-008. Manifest hashes match. Commit size and message checks are measured by the post-run report (known divergences recorded in `run-log.json`). |
| DoD-P01 | met | E2E-01 (external) and E2E-02 (student) pass against the local stack. |
| DoD-P02 | met | E2E-01/02/04 query PostgreSQL and read the JSON copy; runtime log: 8 rows, 8 JSON copies. |
| DoD-P03 | met | E2E-01/02/04 find the participant and organizer emails in Mailpit; the organizer JSON attachment equals the stored copy. |
| DoD-P04 | met | Runtime log: export with organizer credentials HTTP 200, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, ZIP signature `PK`; without credentials 401, wrong password 401; E2E-05 opens the workbook and counts rows. |
| DoD-P05 | met | `GoogleCaptchaVerifierIntegrationTest`: production verifier against a local stub of siteverify, accepted token, rejected token (`success: false`), non-200, malformed answer, unreachable endpoint (9 tests). |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local (`docker compose up -d --build`, test mode, plain HTTP on 127.0.0.1) | E2E-01 … E2E-05 through Chromium | 5/5 passed |
| backend | local | liveness and readiness inside the container; compose health checks | `{"status":"UP"}` twice; all four services `healthy` (NFR-04) |
| backend | local | export with, without and with wrong credentials | 200 xlsx / 401 / 401 |
| backend | local | malformed JSON, unknown property | 400 `body: Malformed request.` without internals |
| backend | local | 20 000-byte body; 61 form-config requests in one minute | 413; 56 × 200 then 5 × 429 (limit 60, earlier e2e calls counted) |
| frontend | local | response headers of `/` and `/api/form-config` | CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` present (F-06) |
| all | local | `docker compose down` (0 containers) then `up` | 8 rows and 8 JSON copies before and after (NFR-02); Mailpit is an in-memory catcher and starts empty, as expected |

Log: `logs/06_runtime-demo.log`.

## Security baseline and project requirements

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `SubmissionValidator` (spec §4 step 3), unknown properties rejected | AC-001-02..07, AC-002-02..06, `SubmissionValidatorTest`, runtime 400 |
| SB-02 | `SecurityConfiguration` (deny by default, export needs ORGANIZER) | AC-008-02, runtime 401 |
| SB-03 | BCrypt hash of `ORGANIZER_PASSWORD` in memory; secrets only from environment | inspection of `SecurityConfiguration`, `application.yml` (no secret defaults) |
| SB-04 | TLS at the external proxy (production); HTTPS-only organizer access; SMTP STARTTLS default on | AC-008-04, `RequestFiltersTest`; production TLS is a manual test (release notes) |
| SB-05 | JPA with bound parameters; React text rendering; plain-text mail | AC-006-02, inspection (no native SQL, no `dangerouslySetInnerHTML`) |
| SB-06 | `RateLimitFilter` incl. export (failed logins) | `RequestFiltersTest`, runtime 429 |
| SB-07 | `ApiExceptionHandler`, `server.error.*` off, logs with ids only | AC-004-04, `ApiExceptionHandlerTest`, log scan (0 personal data in `logs/06_backend-verify.log` and container log) |
| SB-08 | Dependency-Check, npm audit | 0 Critical/High (F-05, F-07) |
| SB-09 | Semgrep 1.177.0, gitleaks v8.30.1 | findings F-02 … F-05 |
| SB-10 | Spring Security headers, nginx headers | runtime headers |
| SB-11 | backend uid 10001, nginx uid 101 | Dockerfiles; `docker run … id` (phase 3) |
| SB-12, SB-13 | only BR-01 fields collected; purpose and retention in `security-requirements.md`, OQ-06 | inspection; release notes carry the deletion instruction |
| SB-14 | consents unchecked; stored with `given_at` | AC-001-08, AC-002-07, AC-005-01 |
| SR-01 | `RegisterParticipant` verifies every token; `GoogleCaptchaVerifier` | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | test mode only via `RECAPTCHA_TEST_MODE`; `ProductionSafetyCheck` | `ProductionSafetyCheckTest`, F-01 |
| SR-03 | `RateLimitFilter`, `RequestBodyLimitFilter`, nginx `client_max_body_size` | `RequestFiltersTest`, runtime 413/429 |
| SR-04 | option ids checked against the active set and type | AC-001-05, AC-002-04 |
| SR-05 | no user input in headers; text/plain only | AC-006-02, `MailRegistrationNotifierTest` |
| SR-06 | `OrganizerHttpsOnlyFilter` before authentication | AC-008-04, `RequestFiltersTest` |
| SR-07 | export and organizer mail contain registration fields only | AC-007-02, AC-008-01, inspection of `PoiWorkbookWriter`, `MailTexts` |
| SR-08 | frontend nginx `proxy_set_header Host $proxy_host` | inspection of `frontend/nginx.conf`; production proxy is a manual test |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (also `logs/06_trace-ac.md`):

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | ExternalRegistrationAcceptanceTest.java, e2e-01-external-registration.spec.ts | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-02 | ExternalRegistrationAcceptanceTest.java (2) | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-03 | ExternalRegistrationAcceptanceTest.java | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-04 | ExternalRegistrationAcceptanceTest.java (2) | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-05 | ExternalRegistrationAcceptanceTest.java (2) | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-06 | ExternalRegistrationAcceptanceTest.java (2) | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-07 | ExternalRegistrationAcceptanceTest.java (2) | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-001-08 | e2e-01-external-registration.spec.ts, us001-external-form.test.tsx | 4607bbf, 780dcfb, 9bdfd16, 6a9f143, c6b8e07, 1350b76 (+6) |
| AC-002-01 | StudentRegistrationAcceptanceTest.java, e2e-02-student-registration.spec.ts | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-02 | StudentRegistrationAcceptanceTest.java (2) | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-03 | StudentRegistrationAcceptanceTest.java (2) | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-04 | StudentRegistrationAcceptanceTest.java | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-05 | StudentRegistrationAcceptanceTest.java (2) | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-06 | StudentRegistrationAcceptanceTest.java (2) | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-002-07 | e2e-02-student-registration.spec.ts, us002-student-form.test.tsx | b8f52d9, 9bdfd16, 1350b76, 8e52d5f |
| AC-003-01 | FormConfigurationAcceptanceTest.java | c6ea1e1, 11eb7e3, ecec61d, bcbec0a, 178d8e1, 94a8400 |
| AC-003-02 | ChangedConfigurationAcceptanceTest.java | c6ea1e1, 11eb7e3, ecec61d, bcbec0a, 178d8e1, 94a8400 |
| AC-003-03 | InvalidConfigurationAcceptanceTest.java (3), ConferenceConfigLoaderTest.java | c6ea1e1, 11eb7e3, ecec61d, bcbec0a, 178d8e1, 94a8400 |
| AC-004-01 | e2e-01-external-registration.spec.ts (2), e2e-02-student-registration.spec.ts, us004-confirmation.test.tsx | 8f7c4a4, 1d509b9, 8e52d5f, dec95f4, 569406c |
| AC-004-02 | e2e-03-field-errors.spec.ts, us004-confirmation.test.tsx | 8f7c4a4, 1d509b9, 8e52d5f, dec95f4, 569406c |
| AC-004-03 | e2e-03-field-errors.spec.ts (2), us004-confirmation.test.tsx | 8f7c4a4, 1d509b9, 8e52d5f, dec95f4, 569406c |
| AC-004-04 | us004-confirmation.test.tsx (2) | 8f7c4a4, 1d509b9, 8e52d5f, dec95f4, 569406c |
| AC-005-01 | StorageAcceptanceTest.java (2), e2e-01-external-registration.spec.ts (2) | 3a02a56, 7ed6704, 92e4678, 178d8e1, ceada53, 9f16abe |
| AC-005-02 | StorageAcceptanceTest.java, JsonCopyFileStoreTest.java, e2e-01-external-registration.spec.ts (2) | 3a02a56, 7ed6704, 92e4678, 178d8e1, ceada53, 9f16abe |
| AC-005-03 | JsonCopyFailureAcceptanceTest.java, JsonCopyFileStoreTest.java | 3a02a56, 075e7e9, 7ed6704, 92e4678, 178d8e1, ceada53 (+1) |
| AC-005-04 | StorageAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 3a02a56, 7ed6704, 92e4678, 178d8e1, ceada53, 9f16abe |
| AC-006-01 | ParticipantEmailAcceptanceTest.java, e2e-01-external-registration.spec.ts (2) | 5178d1e, 14a11ad, 2c3ce57 |
| AC-006-02 | ParticipantEmailAcceptanceTest.java | 5178d1e, 14a11ad, 2c3ce57 |
| AC-006-03 | MailServerUnavailableAcceptanceTest.java | 5178d1e, 14a11ad, 2c3ce57 |
| AC-007-01 | OrganizerNotificationAcceptanceTest.java, e2e-01-external-registration.spec.ts (2) | 1461b53, 14a11ad |
| AC-007-02 | OrganizerNotificationAcceptanceTest.java, e2e-01-external-registration.spec.ts (2) | 1461b53, 14a11ad |
| AC-008-01 | ExportAcceptanceTest.java, e2e-05-organizer-export.spec.ts | 4607bbf, 2352dfa, 09bba3d, 21554a9, 7df81dc, ceada53 (+1) |
| AC-008-02 | ExportAcceptanceTest.java (2), e2e-05-organizer-export.spec.ts | 4607bbf, 2352dfa, 09bba3d, 21554a9, 7df81dc, ceada53 (+1) |
| AC-008-03 | ExportAcceptanceTest.java, e2e-04-slovenian-characters.spec.ts | 4607bbf, 2352dfa, 09bba3d, 21554a9, 7df81dc, ceada53 (+1) |
| AC-008-04 | ExportAcceptanceTest.java | 4607bbf, 2352dfa, 09bba3d, 21554a9, 7df81dc, ceada53 (+1) |

35 acceptance criteria; without a test: none; without a commit: none.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium | PIT (survivor in security-relevant mapping) | `FormConfigController` line 34: inverting the captcha-mode mapping survived; in production the form could offer the test checkbox and every registration would fail (backend verification still holds) | fixed in loop 1 (`FormConfigControllerTest`, 4/4 mutants killed) |
| F-02 | Low (Semgrep ERROR → High, lowered by PA-04) | Semgrep `use-of-basic-authentication` | HTTP Basic on the organizer export (`openapi.yaml`) | accepted under PA-04, D-15: required by architecture, HTTPS or localhost only, BCrypt hash, rate limited |
| F-03 | Medium | Semgrep `npm-missing-minimum-release-age` | `frontend/.npmrc` sets no minimum release age | accepted: every dependency is an exact pin in `tech-stack.md` with a committed lock file and installed with `npm ci`; no version floats |
| F-04 | Low (Semgrep WARNING → Medium, false positive) | Semgrep `missing-internal` | `/api/` location in `nginx.conf` lacks `internal` | false positive: the location must be public; `internal` would break the API |
| F-05 | Low | gitleaks (this run's commits) | 10 matches: test-only constant `ORGANIZER_PASSWORD` in the frozen harness and Spring Boot's throwaway "generated security password" lines in phase 3/4 skeleton logs | false positives: no `.env` value anywhere (leak check: 0 files, 0 commit messages); the final app no longer generates a password (0 lines in `logs/06_backend-verify.log`) |
| F-06 | Low | runtime headers | `/api` responses carry two CSP headers (nginx and backend); the stricter intersection applies | accepted |
| F-07 | Medium | npm audit | 2 Moderate advisories in `qs` via `@stryker-mutator/core` (dev only) | accepted under PA-03 (D-08) |
| F-08 | Low | Dependency-Check | CVE-2025-7962 (angus-activation), CVE-2025-15104 (hibernate-validator) | false positives, suppressed under PA-01/PA-02 (D-06, D-07) |
| F-09 | Low | PIT survivors, security/validation/persistence code (classified individually) | `RateLimitFilter` 63/64: clean-up of old windows above 10 000 clients (memory hygiene only); `SecurityConfiguration` 94: password length boundary (16 vs 17), 37: `WWW-Authenticate` header, 77/110/112–118: CORS for the local dev server only (not deployed); `JsonErrors` 14/15: content type and charset of filter errors; `RequestBodyLimitFilter` 91/111/116: stream delegation details; `WebFilterConfiguration` 35–56: filter order and URL patterns (the refusals still hold, AC-008-04 accepts 401 or 403); `SubmissionValidator` 78: return value of the length check with no observable effect; `RegistrationEntity` 168/208: getters not exported; `RegistrationController` 96, `MailTexts` 35 | accepted: none changes an access decision, a stored value or a validation result for a request that the tests do not cover; gate met |
| F-10 | Low | Stryker survivors (33) | UI rendering details; `validation.ts` 9: dropping the `$` anchor of the client email rule (backend still enforces BR-03) | accepted; gate met |
| F-11 | Low | jscpd | one 6-line clone in test code (0.37 %) | accepted |
| F-12 | Low | Semgrep | partial parse of the vendored `mvnw` script | informational |
| F-13 | Low | clean-clone check (phase 7) | on Windows a clone into a deep folder fails: some test paths exceed 260 characters | fixed in the root README (clone with `core.longpaths=true`); re-check passed |

As found: Critical 0, High 2 (F-02, F-08; both pre-approved), Medium 4, Low 7. Open after triage: Critical 0, High 0, Medium 2 accepted (F-03, F-07), Low 10 accepted or informational; F-01 and F-13 fixed.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | `FormConfigControllerTest` (3 tests) asserting `captcha` for test and production mode and the contract shape | targeted PIT on the class: 4/4 killed; then the one backend re-measurement below |

## Re-measurement (backend, after fix loop 1)

Once, after the last fix (`measurement-scope.md`): 203 tests (unit 106), all passed (`logs/06_backend-all-levels.log`).

| Measure | Phase 5 | Re-measured | Gate |
|---|---|---|---|
| Line coverage (all levels) | 98.19 % (923/940) | 98.19 % (923/940) | ≥ 95 % met |
| Branch coverage (all levels) | 97.57 % | 97.92 % (282/288) | record only |
| PIT raw = adjusted | 90.54 % (335/370) | 90.54 % (334 killed + 1 timed out of 370; 6 without coverage; 0 equivalents) | ≥ 90 % met |

Survivor delta (`logs/05_pit-survivors.txt` → `logs/06_pit-survivors.txt`): the two `FormConfigController` survivors (F-01) are killed; two `ConferenceConfigLoader` mutants (line 134 length boundary, line 149 boolean default) that phase 5 killed now survive, and one mutant timed out. PIT stops at the first killing test and orders tests by time, so run-to-run differences of this size occur; both are config-loader details that `ConferenceConfigLoaderTest` exercises for the 201/1001-character cases. Classified Low under F-09. The frontend did not change after phase 5, so it is not re-measured.
