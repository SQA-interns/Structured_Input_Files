# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261010T2210Z-7c3c`. Raw tool output is in `02_output/logs/phase6-*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | Pass | Final run: backend 197 passed / 0 failed, frontend 61 / 0, e2e 5 / 0 (see "Final run" in `03_test-strategy.md`). `03_acceptance-manifest.sha256`: 26 of 26 hashes match (recomputed after CR removal); the only change to a frozen test was approved by the human (D-15, D-17). |
| DoD-02 | Pass | Backend `./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check`: no violation, 0 SpotBugs bugs (`logs/phase6-backend-check.log`); frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`) clean (`logs/phase6-frontend-check.log`); Semgrep 0 results after F-01 (`logs/phase6-semgrep-rerun.json`). |
| DoD-03 | Pass | Coverage (JaCoCo 0.8.12 / Vitest v8, line and branch, scope = all production code minus the exclusions in `03_test-strategy.md`): backend unit 74.9 % / 86.7 %, non-unit 90.7 % / 69.0 %, all 97.7 % / 93.0 %; frontend unit 97.6 % / 92.6 %, non-unit 92.4 % / 81.0 %, all 100 % / 97.5 % (`logs/phase6-jacoco-*.csv`, `logs/phase6-frontend-coverage-*.log`). Mutation (scope `quality-requirements.md`: all production code minus entry point / bootstrap; tests: each component's full automated suite): backend PIT 1.30.0 with JUnit 5 plugin 1.2.3, all 197 JUnit tests incl. Testcontainers acceptance tests: score 95.4 % (374 killed incl. 2 timed out / 392), scoreCovered 98.2 % (374 / 381), 7 survived, 11 no coverage (`logs/phase6-pitest-final.log`, `logs/phase6-pitest-summary.txt`); frontend Stryker 10.0.0 with Vitest 4.1.11 (D-18..D-20), all 61 Vitest tests (unit and acceptance; the Playwright e2e tests cannot run under Stryker): score 84.7 % (360 killed + 11 timed out / 438), scoreCovered 84.7 %, 67 survived, 0 no coverage (`logs/phase6-stryker.log`). A run under CPU load from PIT gave 90.6 % (more timeouts); the idle-machine run is recorded. Survivors are classified under F-11 and F-12. Thresholds: record only (`quality-requirements.md`). |
| DoD-04 | Pass | `ArchitectureTest` (ArchUnit 1.3.2): 7 rules for the layering declared in spec section 2 (AR-02), JPA only in persistence, no slice cycles (AR-03). AR-01: frontend uses only relative `/api/...` URLs (`src/api.ts`), nginx forwards `/api/`. AR-04: AC-003-02 test. AR-05: AC-005-01..03 tests. AR-06: Flyway V1 + `ddl-auto=validate`. AR-07: AC-001-09 frontend test, no secret in `frontend/` (Gitleaks, leak check). |
| DoD-05 | Pass | No open Critical or High (Findings below). Dependency-Check: 0 open, 2 suppressed false positives (D-04, D-05, D-07); `npm audit`: 0 Critical/High (2 moderate, dev-only, D-06); Semgrep 0; Gitleaks: only F-02 (Low). |
| DoD-06 | Pass | `logs/phase6-runtime-demo.log`: compose stack built and healthy; external and student registration through the frontend origin; rejection of a bad reCAPTCHA token and a duplicate email; rows and JSON copies present; both emails in Mailpit with byte-identical attachment; export 401 without / with wrong credentials, 200 with; data still present after `docker compose down` and `up`. e2e suite in the browser against the same stack: 5 passed. |
| DoD-07 | Pass | Traceability table below: every AC has at least one test and one commit. |
| DoD-08 | Phase 7 | Checked in phase 7 (clone from a clean checkout). |
| DoD-09 | Phase 7 | `release-notes.md` written in phase 7. |
| DoD-10 | Pass | `decisions-log.md`: D-01..D-07, D-15..D-17 resolved; D-08..D-14 and D-16 resolved as pending review; D-18 resolved by D-19 (human: Vitest 4.1.x), D-20 pending review. `00_input-manifest.sha256`: 35 of 35 hashes match. |
| DoD-11 | Pass | Phase 3 commits (`39cff3c`..`f7b94f2`) add tests, test resources and documents only; the freeze commit `7ee5d5a` adds only the manifest; every listed file was committed before it. Build has at least one `feat` commit per user story naming its US/AC IDs (traceability table). Two commits exceed the size guide without a stated reason (recorded as a divergence in `run-log.json`). Manifest hashes match. |
| DoD-P01 | Pass | Runtime demo steps 6–7: external `201` and student `201` through `127.0.0.1:8081/api/registrations`. |
| DoD-P02 | Pass | Runtime demo step 9: both rows, options and consents in PostgreSQL; `registration-<reference>.json` for both in `/data/json-copies` (volume `jsoncopies`). |
| DoD-P03 | Pass | Runtime demo step 10: participant confirmations and organizer notifications in Mailpit; organizer email carries `registration-<reference>.json`, byte-identical to the JSON copy. |
| DoD-P04 | Pass | Runtime demo step 11: 200 with `.xlsx` content type and attachment disposition, workbook opened and both rows read; 401 without credentials and with a wrong password. |
| DoD-P05 | Pass | `ExternalRegistrationAcceptanceTest.ac_001_09_*` against a local mocked siteverify endpoint with `app.recaptcha.test-mode=false` (production code path): accepted token → 201 and verified request parameters; rejected token → 400 `CAPTCHA_FAILED`, nothing stored or emailed. `CaptchaVerifierTest`: non-200, malformed, `success:false`, string `"true"` and unreachable endpoint all reject. |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local (compose, `APP_ENVIRONMENT=local`, test-mode reCAPTCHA, Mailpit) | health and readiness; form configuration; external and student registration; captcha rejection; duplicate rejection; storage; emails; export with and without credentials; restart with `down`/`up` | Pass (`logs/phase6-runtime-demo.log`) |
| frontend | local (compose, nginx unprivileged on 8080 → 127.0.0.1:8081) | page served with security headers; `/api` forwarded; browser flows of the e2e suite (external with Slovenian names, student, field errors, export) | Pass (`logs/phase6-runtime-demo.log`, `logs/phase5-first-run-e2e.log`, `logs/phase6-final-e2e.log`) |
| postgres, mailpit | local | readiness, persistence across recreation (`pgdata`, `jsoncopies` volumes) | Pass |

## Security baseline and project requirements

| Item | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator` (domain, server side) | AC-001-03..11, AC-002-03..04 tests; `RegistrationValidatorTest` |
| SB-02 | `SecurityConfig`: `/api/export/**` requires role ORGANIZER; everything else under `/api` is public by design (`security-requirements.md`) | AC-008-02 tests, runtime demo step 11 |
| SB-03 | Organizer password BCrypt(12) in memory; all secrets from environment, none in code (`application.properties` placeholders without defaults) | Inspection; `StartupGuardTest`; leak check |
| SB-04 | TLS by the external nginx in production; plain HTTP only on 127.0.0.1 locally; SR-06 enforces HTTPS for organizer access | Inspection of `docker-compose.yml` port bindings; AC-008-03 |
| SB-05 | Spring Data JPA with bound parameters; React escaping; plain-text emails; string cells in Excel | Semgrep 0; AC-006-02; `PoiWorkbookWriterTest` (formula text stays a string) |
| SB-06 | `RateLimitFilter`: registrations, exports and every other `/api` request (F-07) | AC-001-13, AC-008-04; `RequestFiltersTest` |
| SB-07 | `ApiExceptionHandler` fixed messages, logs exception types only; filters write the same error body | `assertNoInternals` in AC-004-02, AC-005-02/03; `ApiExceptionHandlerTest.causeChainNamesTypesOnly`; log review below |
| SB-08 | Dependency-Check 12.1.0, `npm audit` | `logs/phase6-dependency-check-report.json`, `logs/phase6-npm-audit*.json` |
| SB-09 | SpotBugs, PMD, Semgrep, Gitleaks | `logs/phase6-backend-check.log`, `logs/phase6-semgrep*.json`, Gitleaks output (F-02) |
| SB-10 | API: CSP `default-src 'none'`, `X-Frame-Options: DENY`, `nosniff`, `no-referrer`, `no-store`; frontend nginx: CSP, framing, content-type options, referrer policy | Runtime demo step 4 |
| SB-11 | Backend image user 10001, frontend nginx user 101 | Runtime demo step 3 |
| SB-12 | Only the fields of BR-01 plus consents | Contracts `openapi.yaml`, `registration-copy.schema.json`; inspection |
| SB-13 | Purposes in `security-requirements.md`; retention D-13 (pending review) | Decisions log |
| SB-14 | Consents unchecked by default, stored with time | AC-001-01, AC-002-01 UI tests; runtime demo step 9 |
| SR-01 | `GoogleRecaptchaVerifier` on the backend for every registration | AC-001-09 (backend), DoD-P05 |
| SR-02 | `StartupGuard`: production refuses test mode or empty keys; test mode only via `APP_RECAPTCHA_TESTMODE` | `StartupGuardTest.unsafeProductionIsRefused` |
| SR-03 | `RateLimitFilter`, `RequestSizeFilter` (16 KiB) | AC-001-13, AC-008-04, `RequestFiltersTest` |
| SR-04 | Option ids checked against the active configured set and audience | AC-001-07, AC-002-04 |
| SR-05 | Control characters rejected; plain-text bodies; headers from configuration only | AC-001-11, AC-006-02, `MailNotifierTest` |
| SR-06 | `HttpsOnlyFilter` (loopback exempt); production refuses HTTPS-only off | AC-008-03, `RequestFiltersTest`, `StartupGuardTest` |
| SR-07 | Organizer email and export contain registration data only | AC-007-02, AC-008-05 |

Logs and error responses: the application logs only exception types, error ids and references (`ApiExceptionHandler.causeChain`, `RegistrationService`); test and demo logs contain only fictitious test data. The `.env` leak check (files and commit messages) printed nothing / `0`; it was re-run after the runtime demonstration log was redacted (F-03).

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `ExternalRegistrationAcceptanceTest.ac_001_01_*`; `external-form.test.tsx` AC-001-01 | `39cff3c`, `d6b7843`; `bc0f68d`, `4ca238d` |
| AC-001-02 | `ac_001_02_*`; e2e `external-registration.spec.ts` | `39cff3c`, `5a93458`; `c5511a7`, `efd17db` |
| AC-001-03 | `ac_001_03_*` (12 cases) | `39cff3c`; `7695451` |
| AC-001-04 | `ac_001_04_*` | `39cff3c`; `7695451` |
| AC-001-05 | `ac_001_05_*` (8 cases) | `39cff3c`; `7695451` |
| AC-001-06 | `ac_001_06_*`; e2e `external-registration.spec.ts` | `39cff3c`, `5a93458`; `efd17db`, `8ed6cea`, `be02e79`, `25082f8` |
| AC-001-07 | `ac_001_07_*` (3 cases) | `39cff3c`; `7695451` |
| AC-001-08 | `ac_001_08_*` | `39cff3c`; `7695451` |
| AC-001-09 | `ac_001_09_*` (3 tests); `external-form.test.tsx` AC-001-09 (2) | `39cff3c`, `d6b7843`; `c5511a7`, `4ca238d` |
| AC-001-10 | `ac_001_10_*` | `39cff3c`; `c5511a7` |
| AC-001-11 | `ac_001_11_*` (8 cases) | `39cff3c`; `7695451` |
| AC-001-12 | `external-form.test.tsx` AC-001-12 (3); e2e `external-registration.spec.ts` | `d6b7843`, `5a93458`; `3949c6a`, `4ca238d` |
| AC-001-13 | `RateLimitAcceptanceTest.ac_001_13_*` (3) | `39cff3c`; `f6b49ba` |
| AC-002-01 | `StudentRegistrationAcceptanceTest.ac_002_01_*`; `student-form.test.tsx`; e2e `student-registration.spec.ts` | `6f678a7`, `bc76251`, `4a6d380`; `4ca238d` |
| AC-002-02 | `ac_002_02_*`; e2e `student-registration.spec.ts` | `6f678a7`, `4a6d380`; `2788c04` |
| AC-002-03 | `ac_002_03_*` (9 cases); `student-form.test.tsx` | `6f678a7`, `bc76251`; `2788c04` |
| AC-002-04 | `ac_002_04_*` (3); `student-form.test.tsx` | `6f678a7`, `b478ab4`, `bc76251`; `2788c04`, `4ca238d` |
| AC-003-01 | `ConferenceOptionsAcceptanceTest.ac_003_01_*` | `22c36ca`; `bc0f68d` |
| AC-003-02 | `ac_003_02_*` | `22c36ca`; `bc0f68d` |
| AC-003-03 | `ac_003_03_*` (4 cases) | `22c36ca`; `bc0f68d` |
| AC-004-01 | `RegistrationConfirmationAcceptanceTest.ac_004_01_*`; `confirmation.test.tsx`; e2e `external-registration.spec.ts` | `c163ec5`, `43e1590`; `63d5338` |
| AC-004-02 | `ac_004_02_*`; `confirmation.test.tsx` (2) | `c163ec5`, `43e1590`; `63d5338` |
| AC-005-01 | `RegistrationStorageAcceptanceTest.ac_005_01_*` | `30c86bd`; `efd17db` |
| AC-005-02 | `ac_005_02_*` | `30c86bd`; `efd17db` |
| AC-005-03 | `DatabaseUnavailableAcceptanceTest.ac_005_03_*` | `30c86bd`; `efd17db` |
| AC-006-01 | `ParticipantEmailAcceptanceTest.ac_006_01_*` (2); e2e both registration specs | `5361954`; `8ed6cea` |
| AC-006-02 | `ac_006_02_*` | `5361954`; `8ed6cea`, `be02e79` |
| AC-006-03 | `MailUnavailableAcceptanceTest.ac_006_03_*` | `5361954`; `8ed6cea` |
| AC-007-01 | `OrganizerNotificationAcceptanceTest.ac_007_01_*`; e2e `external-registration.spec.ts` | `ed1325e`; `be02e79` |
| AC-007-02 | `ac_007_02_*` | `ed1325e`; `be02e79` |
| AC-008-01 | `RegistrationExportAcceptanceTest.ac_008_01_*`; e2e `export.spec.ts`, `external-registration.spec.ts` | `a04606c`, `f7b94f2`; `25082f8` |
| AC-008-02 | `ac_008_02_*` (3); e2e `export.spec.ts` | `a04606c`, `f7b94f2`; `bc0f68d`, `25082f8` |
| AC-008-03 | `ac_008_03_*` | `a04606c`, `e50b423`; `25082f8` |
| AC-008-04 | `RateLimitAcceptanceTest.ac_008_04_*` | `39cff3c`; `f6b49ba`, `25082f8` |
| AC-008-05 | `ac_008_05_*` | `a04606c`; `25082f8` |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium | Semgrep `generic.nginx.security.request-host-used` (WARNING) | `frontend/nginx.conf` forwarded the client-supplied `Host` header to the backend | Fixed in `e9c374f` (header no longer forwarded); Semgrep re-run 0 results |
| F-02 | Low | Gitleaks (history and tree) | 9 `generic-api-key` matches in `logs/phase3-backend-acceptance-run.log`: the random password Spring Boot generates for the bootstrap skeleton's default user ("Using generated security password"), different in every test context, never a configured secret; the `.env` leak check is clean | Accepted: no real credential; history is not rewritten (`working-rules.md`); later logs do not contain such lines because the application defines its own user store |
| F-03 | Low | Phase 6 log review (step 8) | The runtime demonstration output contained the `ORGANIZER_EMAILS` value (Mailpit recipient list) | Fixed before commit: value replaced by `<ORGANIZER_EMAILS>` in `logs/phase6-runtime-demo.log`; leak check re-run clean |
| F-04 | Medium | `npm audit` | 2 moderate advisories in `qs` 6.15.1 via `typed-rest-client` via `@stryker-mutator/core` (dev-only, not in the bundle; `npm audit --omit=dev`: 0) | Accepted (D-06): dev tooling only |
| F-05 | Low | Dependency-Check | CVE-2025-7962 (angus-activation) and CVE-2025-15104 (hibernate-validator) are CPE false positives | Suppressed per jar (D-04, D-05, D-07); 0 open |
| F-06 | Medium | Stryker, D-18 | The pinned `vitest-runner` 10.0.0 did not activate runtime mutants under the pinned Vitest 5.0.3 (invalid 24.2 %) | Fixed by the human-approved change to Vitest 4.1.11 (D-19, D-20, `bd0cbf1` build commit); valid score 84.7 % |
| F-07 | Medium | Security review (SB-06) | `GET /api/form-config` (public) had no rate limit; only registration and export were limited | Fixed in `0569fb4`: every other `/api` request limited per client (120 / min, configurable) |
| F-08 | Low | PIT survivors in `FixedWindowRateLimiter` (security code) | Cleanup of expired windows untested: a mutant resetting active counters survived | Fixed in `45ab2d1` (new test); targeted PIT re-run kills the non-equivalent mutants; 3 remaining are equivalent (cleanup boundary affects memory only) |
| F-09 | Low | Final PIT survivors (backend, 7) | `RegistrationValidator.checked`/`email` `return null` → `""` on paths that always throw (equivalent); `LimitedStream.isFinished`/`isReady` delegates (not used by blocking reads); `FileJsonCopyStore` `force`/`close` (durability not observable in tests); rate-limiter cleanup boundary (equivalent). No coverage (11): test-mode captcha bean, CORS branch (local only), `Thread.interrupt` path, access-denied handler (no non-organizer authenticated user exists) | Accepted with the reasons given; none is a validation or persistence decision a test could observe |
| F-10 | Low | Stryker survivor in `validation.ts` | Removing the `# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Run `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261010T2210Z-7c3c`. Raw tool output is in `02_output/logs/phase6-*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | Pass | Final run: backend 197 passed / 0 failed, frontend 61 / 0, e2e 5 / 0 (see "Final run" in `03_test-strategy.md`). `03_acceptance-manifest.sha256`: 26 of 26 hashes match (recomputed after CR removal); the only change to a frozen test was approved by the human (D-15, D-17). |
| DoD-02 | Pass | Backend `./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check`: no violation, 0 SpotBugs bugs (`logs/phase6-backend-check.log`); frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`) clean (`logs/phase6-frontend-check.log`); Semgrep 0 results after F-01 (`logs/phase6-semgrep-rerun.json`). |
| DoD-03 | Pass | Coverage (JaCoCo 0.8.12 / Vitest v8, line and branch, scope = all production code minus the exclusions in `03_test-strategy.md`): backend unit 74.9 % / 86.7 %, non-unit 90.7 % / 69.0 %, all 97.7 % / 93.0 %; frontend unit 97.6 % / 92.6 %, non-unit 92.4 % / 81.0 %, all 100 % / 97.5 % (`logs/phase6-jacoco-*.csv`, `logs/phase6-frontend-coverage-*.log`). Mutation (scope `quality-requirements.md`: all production code minus entry point / bootstrap; tests: each component's full automated suite): backend PIT 1.30.0 with JUnit 5 plugin 1.2.3, all 197 JUnit tests incl. Testcontainers acceptance tests: score 95.4 % (374 killed incl. 2 timed out / 392), scoreCovered 98.2 % (374 / 381), 7 survived, 11 no coverage (`logs/phase6-pitest-final.log`, `logs/phase6-pitest-summary.txt`); frontend Stryker 10.0.0 with Vitest 4.1.11 (D-18..D-20), all 61 Vitest tests (unit and acceptance; the Playwright e2e tests cannot run under Stryker): score 84.7 % (360 killed + 11 timed out / 438), scoreCovered 84.7 %, 67 survived, 0 no coverage (`logs/phase6-stryker.log`). A run under CPU load from PIT gave 90.6 % (more timeouts); the idle-machine run is recorded. Survivors are classified under F-11 and F-12. Thresholds: record only (`quality-requirements.md`). |
| DoD-04 | Pass | `ArchitectureTest` (ArchUnit 1.3.2): 7 rules for the layering declared in spec section 2 (AR-02), JPA only in persistence, no slice cycles (AR-03). AR-01: frontend uses only relative `/api/...` URLs (`src/api.ts`), nginx forwards `/api/`. AR-04: AC-003-02 test. AR-05: AC-005-01..03 tests. AR-06: Flyway V1 + `ddl-auto=validate`. AR-07: AC-001-09 frontend test, no secret in `frontend/` (Gitleaks, leak check). |
| DoD-05 | Pass | No open Critical or High (Findings below). Dependency-Check: 0 open, 2 suppressed false positives (D-04, D-05, D-07); `npm audit`: 0 Critical/High (2 moderate, dev-only, D-06); Semgrep 0; Gitleaks: only F-02 (Low). |
| DoD-06 | Pass | `logs/phase6-runtime-demo.log`: compose stack built and healthy; external and student registration through the frontend origin; rejection of a bad reCAPTCHA token and a duplicate email; rows and JSON copies present; both emails in Mailpit with byte-identical attachment; export 401 without / with wrong credentials, 200 with; data still present after `docker compose down` and `up`. e2e suite in the browser against the same stack: 5 passed. |
| DoD-07 | Pass | Traceability table below: every AC has at least one test and one commit. |
| DoD-08 | Phase 7 | Checked in phase 7 (clone from a clean checkout). |
| DoD-09 | Phase 7 | `release-notes.md` written in phase 7. |
| DoD-10 | Pass | `decisions-log.md`: D-01..D-07, D-15..D-17 resolved; D-08..D-14 and D-16 resolved as pending review; D-18 resolved by D-19 (human: Vitest 4.1.x), D-20 pending review. `00_input-manifest.sha256`: 35 of 35 hashes match. |
| DoD-11 | Pass | Phase 3 commits (`39cff3c`..`f7b94f2`) add tests, test resources and documents only; the freeze commit `7ee5d5a` adds only the manifest; every listed file was committed before it. Build has at least one `feat` commit per user story naming its US/AC IDs (traceability table). Two commits exceed the size guide without a stated reason (recorded as a divergence in `run-log.json`). Manifest hashes match. |
| DoD-P01 | Pass | Runtime demo steps 6–7: external `201` and student `201` through `127.0.0.1:8081/api/registrations`. |
| DoD-P02 | Pass | Runtime demo step 9: both rows, options and consents in PostgreSQL; `registration-<reference>.json` for both in `/data/json-copies` (volume `jsoncopies`). |
| DoD-P03 | Pass | Runtime demo step 10: participant confirmations and organizer notifications in Mailpit; organizer email carries `registration-<reference>.json`, byte-identical to the JSON copy. |
| DoD-P04 | Pass | Runtime demo step 11: 200 with `.xlsx` content type and attachment disposition, workbook opened and both rows read; 401 without credentials and with a wrong password. |
| DoD-P05 | Pass | `ExternalRegistrationAcceptanceTest.ac_001_09_*` against a local mocked siteverify endpoint with `app.recaptcha.test-mode=false` (production code path): accepted token → 201 and verified request parameters; rejected token → 400 `CAPTCHA_FAILED`, nothing stored or emailed. `CaptchaVerifierTest`: non-200, malformed, `success:false`, string `"true"` and unreachable endpoint all reject. |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local (compose, `APP_ENVIRONMENT=local`, test-mode reCAPTCHA, Mailpit) | health and readiness; form configuration; external and student registration; captcha rejection; duplicate rejection; storage; emails; export with and without credentials; restart with `down`/`up` | Pass (`logs/phase6-runtime-demo.log`) |
| frontend | local (compose, nginx unprivileged on 8080 → 127.0.0.1:8081) | page served with security headers; `/api` forwarded; browser flows of the e2e suite (external with Slovenian names, student, field errors, export) | Pass (`logs/phase6-runtime-demo.log`, `logs/phase5-first-run-e2e.log`, `logs/phase6-final-e2e.log`) |
| postgres, mailpit | local | readiness, persistence across recreation (`pgdata`, `jsoncopies` volumes) | Pass |

## Security baseline and project requirements

| Item | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator` (domain, server side) | AC-001-03..11, AC-002-03..04 tests; `RegistrationValidatorTest` |
| SB-02 | `SecurityConfig`: `/api/export/**` requires role ORGANIZER; everything else under `/api` is public by design (`security-requirements.md`) | AC-008-02 tests, runtime demo step 11 |
| SB-03 | Organizer password BCrypt(12) in memory; all secrets from environment, none in code (`application.properties` placeholders without defaults) | Inspection; `StartupGuardTest`; leak check |
| SB-04 | TLS by the external nginx in production; plain HTTP only on 127.0.0.1 locally; SR-06 enforces HTTPS for organizer access | Inspection of `docker-compose.yml` port bindings; AC-008-03 |
| SB-05 | Spring Data JPA with bound parameters; React escaping; plain-text emails; string cells in Excel | Semgrep 0; AC-006-02; `PoiWorkbookWriterTest` (formula text stays a string) |
| SB-06 | `RateLimitFilter`: registrations, exports and every other `/api` request (F-07) | AC-001-13, AC-008-04; `RequestFiltersTest` |
| SB-07 | `ApiExceptionHandler` fixed messages, logs exception types only; filters write the same error body | `assertNoInternals` in AC-004-02, AC-005-02/03; `ApiExceptionHandlerTest.causeChainNamesTypesOnly`; log review below |
| SB-08 | Dependency-Check 12.1.0, `npm audit` | `logs/phase6-dependency-check-report.json`, `logs/phase6-npm-audit*.json` |
| SB-09 | SpotBugs, PMD, Semgrep, Gitleaks | `logs/phase6-backend-check.log`, `logs/phase6-semgrep*.json`, Gitleaks output (F-02) |
| SB-10 | API: CSP `default-src 'none'`, `X-Frame-Options: DENY`, `nosniff`, `no-referrer`, `no-store`; frontend nginx: CSP, framing, content-type options, referrer policy | Runtime demo step 4 |
| SB-11 | Backend image user 10001, frontend nginx user 101 | Runtime demo step 3 |
| SB-12 | Only the fields of BR-01 plus consents | Contracts `openapi.yaml`, `registration-copy.schema.json`; inspection |
| SB-13 | Purposes in `security-requirements.md`; retention D-13 (pending review) | Decisions log |
| SB-14 | Consents unchecked by default, stored with time | AC-001-01, AC-002-01 UI tests; runtime demo step 9 |
| SR-01 | `GoogleRecaptchaVerifier` on the backend for every registration | AC-001-09 (backend), DoD-P05 |
| SR-02 | `StartupGuard`: production refuses test mode or empty keys; test mode only via `APP_RECAPTCHA_TESTMODE` | `StartupGuardTest.unsafeProductionIsRefused` |
| SR-03 | `RateLimitFilter`, `RequestSizeFilter` (16 KiB) | AC-001-13, AC-008-04, `RequestFiltersTest` |
| SR-04 | Option ids checked against the active configured set and audience | AC-001-07, AC-002-04 |
| SR-05 | Control characters rejected; plain-text bodies; headers from configuration only | AC-001-11, AC-006-02, `MailNotifierTest` |
| SR-06 | `HttpsOnlyFilter` (loopback exempt); production refuses HTTPS-only off | AC-008-03, `RequestFiltersTest`, `StartupGuardTest` |
| SR-07 | Organizer email and export contain registration data only | AC-007-02, AC-008-05 |

Logs and error responses: the application logs only exception types, error ids and references (`ApiExceptionHandler.causeChain`, `RegistrationService`); test and demo logs contain only fictitious test data. The `.env` leak check (files and commit messages) printed nothing / `0`; it was re-run after the runtime demonstration log was redacted (F-03).

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `ExternalRegistrationAcceptanceTest.ac_001_01_*`; `external-form.test.tsx` AC-001-01 | `39cff3c`, `d6b7843`; `bc0f68d`, `4ca238d` |
| AC-001-02 | `ac_001_02_*`; e2e `external-registration.spec.ts` | `39cff3c`, `5a93458`; `c5511a7`, `efd17db` |
| AC-001-03 | `ac_001_03_*` (12 cases) | `39cff3c`; `7695451` |
| AC-001-04 | `ac_001_04_*` | `39cff3c`; `7695451` |
| AC-001-05 | `ac_001_05_*` (8 cases) | `39cff3c`; `7695451` |
| AC-001-06 | `ac_001_06_*`; e2e `external-registration.spec.ts` | `39cff3c`, `5a93458`; `efd17db`, `8ed6cea`, `be02e79`, `25082f8` |
| AC-001-07 | `ac_001_07_*` (3 cases) | `39cff3c`; `7695451` |
| AC-001-08 | `ac_001_08_*` | `39cff3c`; `7695451` |
| AC-001-09 | `ac_001_09_*` (3 tests); `external-form.test.tsx` AC-001-09 (2) | `39cff3c`, `d6b7843`; `c5511a7`, `4ca238d` |
| AC-001-10 | `ac_001_10_*` | `39cff3c`; `c5511a7` |
| AC-001-11 | `ac_001_11_*` (8 cases) | `39cff3c`; `7695451` |
| AC-001-12 | `external-form.test.tsx` AC-001-12 (3); e2e `external-registration.spec.ts` | `d6b7843`, `5a93458`; `3949c6a`, `4ca238d` |
| AC-001-13 | `RateLimitAcceptanceTest.ac_001_13_*` (3) | `39cff3c`; `f6b49ba` |
| AC-002-01 | `StudentRegistrationAcceptanceTest.ac_002_01_*`; `student-form.test.tsx`; e2e `student-registration.spec.ts` | `6f678a7`, `bc76251`, `4a6d380`; `4ca238d` |
| AC-002-02 | `ac_002_02_*`; e2e `student-registration.spec.ts` | `6f678a7`, `4a6d380`; `2788c04` |
| AC-002-03 | `ac_002_03_*` (9 cases); `student-form.test.tsx` | `6f678a7`, `bc76251`; `2788c04` |
| AC-002-04 | `ac_002_04_*` (3); `student-form.test.tsx` | `6f678a7`, `b478ab4`, `bc76251`; `2788c04`, `4ca238d` |
| AC-003-01 | `ConferenceOptionsAcceptanceTest.ac_003_01_*` | `22c36ca`; `bc0f68d` |
| AC-003-02 | `ac_003_02_*` | `22c36ca`; `bc0f68d` |
| AC-003-03 | `ac_003_03_*` (4 cases) | `22c36ca`; `bc0f68d` |
| AC-004-01 | `RegistrationConfirmationAcceptanceTest.ac_004_01_*`; `confirmation.test.tsx`; e2e `external-registration.spec.ts` | `c163ec5`, `43e1590`; `63d5338` |
| AC-004-02 | `ac_004_02_*`; `confirmation.test.tsx` (2) | `c163ec5`, `43e1590`; `63d5338` |
| AC-005-01 | `RegistrationStorageAcceptanceTest.ac_005_01_*` | `30c86bd`; `efd17db` |
| AC-005-02 | `ac_005_02_*` | `30c86bd`; `efd17db` |
| AC-005-03 | `DatabaseUnavailableAcceptanceTest.ac_005_03_*` | `30c86bd`; `efd17db` |
| AC-006-01 | `ParticipantEmailAcceptanceTest.ac_006_01_*` (2); e2e both registration specs | `5361954`; `8ed6cea` |
| AC-006-02 | `ac_006_02_*` | `5361954`; `8ed6cea`, `be02e79` |
| AC-006-03 | `MailUnavailableAcceptanceTest.ac_006_03_*` | `5361954`; `8ed6cea` |
| AC-007-01 | `OrganizerNotificationAcceptanceTest.ac_007_01_*`; e2e `external-registration.spec.ts` | `ed1325e`; `be02e79` |
| AC-007-02 | `ac_007_02_*` | `ed1325e`; `be02e79` |
| AC-008-01 | `RegistrationExportAcceptanceTest.ac_008_01_*`; e2e `export.spec.ts`, `external-registration.spec.ts` | `a04606c`, `f7b94f2`; `25082f8` |
| AC-008-02 | `ac_008_02_*` (3); e2e `export.spec.ts` | `a04606c`, `f7b94f2`; `bc0f68d`, `25082f8` |
| AC-008-03 | `ac_008_03_*` | `a04606c`, `e50b423`; `25082f8` |
| AC-008-04 | `RateLimitAcceptanceTest.ac_008_04_*` | `39cff3c`; `f6b49ba`, `25082f8` |
| AC-008-05 | `ac_008_05_*` | `a04606c`; `25082f8` |

## Findings

 anchor of the email domain pattern survived (client accepted `ana@example.si1`; the backend rejected it) | Fixed in `692c3ef` (test cases); Stryker re-run kills it |
| F-11 | Low | Stryker survivors (frontend, 67) | UI glue: input `type`/`autoComplete`/CSS literals and initial state (equivalent), Google widget render/reset guards (only reachable with the real reCAPTCHA library), stale errors not cleared between two submissions, `some` vs `every` for unplaceable field errors, loading text while the load error shows | Accepted: cosmetic or covered by the manual real-reCAPTCHA test in the release notes |
| F-12 | Low | Process (commit size) | Two phase 4 commits exceed the size guide without a stated reason (`bc0f68d` 21 files, `efd17db` 675 lines) | Accepted; recorded as a divergence; history not rewritten |

Severity counts: Critical 0, High 0, Medium 4 (F-01, F-04, F-06, F-07), Low 8. Open at end: Critical 0, High 0, Medium 0 (F-04 accepted with a decision).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | Drop `proxy_set_header Host` in `frontend/nginx.conf` (2026-10-10T23:28Z–23:30Z) | Semgrep 0 results (`logs/phase6-semgrep-rerun.json`); runtime demo and e2e on the rebuilt image pass |
| 2 | F-07, F-08 | Public-API rate limit bucket; rate-limiter cleanup test (23:38Z–23:50Z) | Affected unit and acceptance tests pass (`logs/phase6-fix-f07-f08.log`); targeted PIT (`logs/phase6-pitest-f08-recheck.log`); full suite and final PIT on the new code |
| 3 | F-06 | Vitest 4.1.11 after human approval (D-19, D-20) (2026-10-10T23:55Z–2026-10-11T00:12Z) | Check, build, 59 tests, audit; Stryker produces a valid score |
| 4 | F-10 | Email TLD test cases (2026-10-11T00:19Z–00:23Z) | Stryker re-run: mutant killed (`logs/phase6-stryker.log`) |
| 5 | F-03 | Redact the `.env` value from the demo log before commit (2026-10-10T23:35Z) | Leak check (files, commit messages, per address) prints nothing / 0 |
