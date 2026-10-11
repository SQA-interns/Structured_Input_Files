# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-11 at the commit that adds this report. Raw outputs are in `02_output/logs/phase6-*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | Pass | Final run: backend 189 tests (87 unit, 6 architecture, 3 integration, 93 acceptance), 0 failed (`phase6-backend-all.log`); frontend 27 (20 unit, 7 acceptance), 0 failed (`phase6-frontend-all.log`); e2e 2/2 against the compose stack (`phase6-e2e.log`). All 32 hashes in `03_acceptance-manifest.sha256` match (recomputed with LF normalisation); the manifest changed once, by the human-approved D-12/D-13. |
| DoD-02 | Pass | Backend `./mvnw -Pcheck verify`: Spotless, PMD, CPD clean; SpotBugs 0 bugs after 22 fixes and one scoped exclusion (D-14) (`phase6-backend-all.log`). Frontend `npm run check`: Prettier, ESLint, `tsc --noEmit` clean (`phase6-frontend-check.log`). Semgrep findings triaged below. |
| DoD-03 | Pass (record only) | Coverage and mutation score, re-measured once after the last fix, are listed below with their scope; thresholds are "record only" (`quality-requirements.md`). |
| DoD-04 | Pass | `ArchitectureTest`: 6 ArchUnit rules for the specification's section 2 declaration (AR-02), including the slice cycle check (AR-03), all pass. Flyway-only schema: `ddl-auto=validate` (AR-06). |
| DoD-05 | Pass | No open Critical or High finding (findings table). |
| DoD-06 | Pass | Runtime demonstration below (`phase6-runtime-demo.log`). |
| DoD-07 | Pass | Traceability table: every AC has at least one test and one commit. |
| DoD-08 | Checked in phase 7 | The READMEs are written in phase 7 and followed from a clean clone there. |
| DoD-09 | Checked in phase 7 | `docs/release-notes.md`, phase 7. |
| DoD-10 | Pass | Decisions D-01 … D-15 each have a resolution or are pending review (D-05 … D-10). The 36 hashes in `00_input-manifest.sha256` match. |
| DoD-11 | Pass | Phase 3 commits added only tests, test resources, the test-only dependency (D-11) and docs; the freeze commit adds only the manifest, and every file it lists was committed earlier in phase 3. Each user story has at least one `feat` commit naming it (traceability). Commits over the size guide: US-003 (567 lines) and US-008 (with its log), with no reason stated in the message (F-12). Every manifest hash matches. |
| DoD-P01 | Pass | Demo: one external and one student registration through `http://127.0.0.1:8081/api`, both HTTP 201. |
| DoD-P02 | Pass | Demo: both rows, their options and consents in PostgreSQL (`psql`); both `<id>.json` files on the `jsoncopies` volume. |
| DoD-P03 | Pass | Demo: Mailpit search shows both participant confirmations and both organizer notifications, each organizer mail with 1 attachment. |
| DoD-P04 | Pass | Demo: export with organizer credentials HTTP 200, `.xlsx` with both registrations' Slovenian names in `sharedStrings.xml`; without and with wrong credentials HTTP 401 (backend and frontend proxy). |
| DoD-P05 | Pass | `RecaptchaProductionPathAcceptanceTest` (4 tests): accepted token, rejected token (400, nothing stored), verifier error (503, nothing stored), test token refused in production mode; mocked endpoint. Unit: `CaptchaVerifierTest`. |

## Runtime demonstration

Local stack `docker compose --env-file ../.env up -d --build` (images from `tech-stack.md`), script output in `phase6-runtime-demo.log`.

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend | local compose, `APP_ENVIRONMENT=local` | readiness and liveness; registration through the frontend proxy; rejection without consent (0 rows); export with, without and with wrong credentials | UP/UP; 201, 201; 400 and nothing stored; 200 / 401 / 401 |
| frontend | local compose, nginx as uid 101 | page served (200) with CSP, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`; `/api/form-config` through the proxy | as expected |
| PostgreSQL + JSON copy volume | local compose | rows, options, consents; `ls` of the copies; `docker compose down` then `up` | data present before and after recreation (NFR-02) |
| Mailpit | local compose | `/api/v1/search` for the demo marker | 4 messages, organizer mails with the JSON attachment |
| full stack | browser (Playwright) | student with Slovenian characters and external participant, end to end (NFR-01) | 2/2 passed |

Backend logs contained no personal data of the demo (0 matching lines); both application containers run as non-root (uid 100 and 101).

## Security

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator`, Jackson unknown-property rejection, catalog validation | acceptance AC-001-02…07, AC-002-02…06; `RegistrationValidatorTest` |
| SB-02 | `SecurityConfig` (public form endpoints, organizer-only export, deny all else) | AC-008-02; integration test (403 for other paths) |
| SB-03 | BCrypt hash of `ORGANIZER_PASSWORD` at startup, ≥ 16 characters; secrets only from environment | `ConfigChecksTest`; inspection of `application.properties` (no secret defaults) |
| SB-04 | TLS at the production proxy (`deploy/nginx-production.conf.example`), HTTPS-only organizer guard | AC-008-04; `nginx -t` on the example |
| SB-05 | JPA parameters only; React escaping; plain-text email; string cells in the workbook | `ExcelRegistrationExporterTest.formulaLikeInputStaysText`, AC-006-02; inspection |
| SB-06 | `RequestLimitFilter` per client and endpoint group (also limits failed logins) | `RateLimitAcceptanceTest`, `RequestFiltersTest` |
| SB-07 | `ApiExceptionHandler`, filter error writer, logs with registration ids only | `HttpHardeningAcceptanceTest.sb_07…`; demo log check |
| SB-08 | dependency scans | Dependency-Check 12.1.0 and `npm audit` (findings F-05 … F-07) |
| SB-09 | static analysis and secret scan | Semgrep 1.177.0, Gitleaks v8.30.1, SpotBugs, PMD (findings) |
| SB-10 | Spring Security headers (CSP, frame, content-type, referrer) and frontend nginx headers | `HttpHardeningAcceptanceTest.sb_10…`; demo `curl -I` |
| SB-11 | non-root users in both images | demo `id -u` |
| SB-12, SB-13 | only the fields in `security-requirements.md`; retention D-10 | inspection of the schema and JSON contract; release notes |
| SB-14 | consents never preselected, mandatory consent enforced on the server | AC-001-06, AC-001-08, AC-002-07 |
| SR-01 | server-side verification on every registration | `RecaptchaProductionPathAcceptanceTest`, AC-001-07, AC-002-06 |
| SR-02 | `StartupSafetyChecks` | `ProductionStartupAcceptanceTest`, `ConfigChecksTest` |
| SR-03 | rate limits per client for all three `/api` endpoints, 16 KiB body limit | `RateLimitAcceptanceTest`, `HttpHardeningAcceptanceTest.sr_03…`, `RequestFiltersTest` |
| SR-04 | options checked against the active, type-available set | AC-001-05, AC-002-04 |
| SR-05 | plain-text mail, fixed subject, strict email rule | AC-006-02, `MailRegistrationNotifierTest` |
| SR-06 | `OrganizerTransportGuard` before authentication | AC-008-04, `RequestFiltersTest` |
| SR-07 | JSON copy, export and organizer mail contain registration data only | AC-005-02 (no captcha token), AC-007-02, `FileJsonCopyStoreTest`; inspection |
| SR-08 | fixed `proxy_set_header Host` in the frontend nginx and the production example | inspection; Semgrep re-scan after F-01 |

Secret leak check (`verify-release` step 8): file check listed no file; commit-message check printed `0`. Committed logs and error responses were checked for personal data: the demo log masks the organizer address from `.env`; test logs contain only generated test identities (`@example.test`).

## Coverage, mutation and size (final, after the last fix)

| Component | Coverage level | Tests | Line | Branch |
|---|---|---|---|---|
| backend (JaCoCo 0.8.12) | unit | 93 (87 unit + 6 architecture) | 73.2 % (608/831) | 86.2 % (225/261) |
| backend | nonUnit | 96 (93 acceptance + 3 integration) | 91.6 % (761/831) | 75.1 % (196/261) |
| backend | all | 189 | 99.0 % (823/831) | 93.1 % (243/261) |
| frontend (Vitest v8) | unit | 20 | 87.9 % (123/140) | 81.0 % (81/100) |
| frontend | nonUnit | 7 acceptance (e2e not instrumented) | 84.3 % (118/140) | 77.0 % (77/100) |
| frontend | all | 27 | 90.0 % (126/140) | 96.0 % (96/100) |

| Component | Tool | Scope | Tests | Mutants | Killed | Covered | score | scoreCovered |
|---|---|---|---|---|---|---|---|---|
| backend | PIT 1.30.0 | `si.konferenca.registration.*` except `RegistrationApplication` | full suite (189) | 320 | 320 | 320 | 100 % | 100 % |
| frontend | Stryker 10.0.0 | `src/**` except tests, `*.d.ts`, `main.tsx`, `test-setup.ts` | Vitest suite (27); e2e excluded (D-15) | 354 | 304 (294 + 10 timeout) | 339 | 85.9 % | 89.7 % |

Remaining frontend survivors (34 survived, 15 uncovered, 1 runtime error): none in `validation.ts`; the rest are in the reCAPTCHA loader (needs Google's script, manual test), request `Accept` headers and presentation text (F-11).

Code size (cloc image `aldanial/cloc:2.10`, D-01; code lines): backend production 1,922 (52 files incl. resources), backend tests 3,022; frontend production 533, frontend tests 757 (`tests/` and `playwright.config.ts` 489, `src/*.test.*` 268). Duplication: CPD clean (backend); jscpd 1.45 % (2 clones, 13 lines, frontend `src`).

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | ExternalRegistrationAcceptanceTest, registration.e2e.spec | 29eead9, 45537e1, cc4b22c |
| AC-001-02 … AC-001-07 | ExternalRegistrationAcceptanceTest (AC-001-07 also RecaptchaProductionPathAcceptanceTest) | 29eead9, 45537e1 |
| AC-001-08 | externalForm.acceptance.test | cc4b22c |
| AC-002-01 … AC-002-06 | StudentRegistrationAcceptanceTest | 29eead9, 45537e1 |
| AC-002-07 | studentForm.acceptance.test | cc4b22c |
| AC-003-01 … AC-003-03 | ConferenceOptionsAcceptanceTest | 1a73cc9 |
| AC-004-01 | confirmation.acceptance.test, registration.e2e.spec | aab8372, cc4b22c |
| AC-004-02 … AC-004-04 | confirmation.acceptance.test | aab8372, cc4b22c |
| AC-005-01, AC-005-02, AC-005-04 | RegistrationStorageAcceptanceTest | 615ee67, 67e5ed9, e0f0052 |
| AC-005-03 | JsonCopyFailureAcceptanceTest | 67e5ed9 |
| AC-006-01, AC-006-02 | ParticipantEmailAcceptanceTest | b1aaf97 |
| AC-006-03 | MailServerDownAcceptanceTest | b1aaf97 |
| AC-007-01, AC-007-02 | OrganizerNotificationAcceptanceTest | b1aaf97 |
| AC-008-01 … AC-008-04 | ExportAcceptanceTest | f308833, e0f0052 |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Medium (Semgrep WARNING) | Semgrep `request-host-used` | production proxy example redirected with the client's `Host` | fixed (loop 1), re-scan clean |
| F-02 | Medium | Semgrep `npm-missing-minimum-release-age` | `.npmrc` has no minimum release age | accepted: exact pins, committed lock file, `npm ci`, scanned in phase 0 and 6 |
| F-03 | Low (reported WARNING) | Semgrep `header-redefinition` ×4 | frontend `nginx.conf` `add_header` in `location /` | false positive: no `http`/`server`-level `add_header` exists to be dropped |
| F-04 | Low | Gitleaks `generic-api-key` ×23 | committed test logs contain Spring Boot's "Using generated security password" | false positive: random per-JVM test password of the skeleton, never configured or reused; `.env` leak check clean |
| F-05 | High (CVSS 7.5) → Low | Dependency-Check | CVE-2025-7962 on `angus-activation` 2.0.3 | false positive, suppressed per PA-01 (D-03); shipped `angus-mail` 2.0.5 |
| F-06 | Medium (CVSS 5.3) → Low | Dependency-Check | CVE-2025-15104 on `hibernate-validator` | false positive per PA-02 (D-03) |
| F-07 | Medium | `npm audit` | 2 Moderate (`qs` via `typed-rest-client` via Stryker), dev only | accepted per PA-03 (D-04); `--omit=dev` 0 |
| F-08 | Medium → Low | SpotBugs `EI_EXPOSE_REP2` | injected collaborator in `RegistrationService` | false positive, scoped exclusion (D-14); 22 other `EI_EXPOSE_REP` fixed in phase 4 |
| F-09 | Low | Stryker survivors | form-unavailable state, general error without field errors and email input type untested | fixed (loop 2) with new tests |
| F-10 | Medium | phase 5 integration test | export showed a consent once per selected option (missing value equality on stored rows) | fixed in phase 5 (`e0f0052`) |
| F-11 | Low | Stryker survivors | reCAPTCHA loader, `Accept` headers and texts not killed | accepted: loader is covered by the manual test with real keys (release notes) |
| F-12 | Low | process | commit `f6d37e6` mixes docs into a build commit; US-003 commit 567 lines without a stated reason | accepted; history is not rewritten (recorded as divergences) |

Not reported: Semgrep did not report HTTP Basic on the organizer endpoint, so PA-04 was not needed.

As found: Critical 0, High 1, Medium 7, Low 4. Open after triage: Critical 0, High 0, Medium 0.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | fixed host in the HTTPS redirect of `deploy/nginx-production.conf.example` (`92be335`) | Semgrep `p/nginx` re-scan: 0 results; `nginx -t` passes |
| 2 | F-09 | `App.test.tsx`, extra `RegistrationForm` test (`5665c7c`) | Vitest 27/27; Stryker re-measure 85.9 % |
