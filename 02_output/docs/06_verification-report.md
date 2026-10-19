# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-19 on branch `kyuhi/single-agent/tdd/v001`. Production code and tests are unchanged since phase 5, so coverage and mutation score were not re-measured (`general/quality/measurement-scope.md`). Raw outputs: `out/logs/phase6-*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Final run: backend 264 passed / 0 failed, frontend 80 / 0, e2e 5 / 0 (`phase6-backend-full.log`, `phase6-frontend-test.log`, `phase6-e2e-final.log`). Hashes: 33 frozen files and 41 input files match (`phase6-manifest-check.log`) |
| DoD-02 | met | Backend: Spotless, PMD, CPD, SpotBugs 0 bugs (D-11). Frontend: Prettier, ESLint, `tsc` for `src` and `e2e`. All clean (`phase6-backend-full.log`, `phase6-frontend-check.log`). Semgrep: one Medium, accepted (F-02) |
| DoD-03 | met | Quality gate met per component after 1 survivor round each: backend line 97.64 %, mutation adjusted 100 % (raw 99.10 %, 331/334); frontend line 100 %, mutation adjusted 100 % (raw 98.10 %, 362/369). Scope, tools, rounds and equivalent mutants: `docs/03_test-strategy.md` |
| DoD-04 | met | `ArchitectureTest`: 6 ArchUnit rules (AR-02 layers, AR-03 no slice cycles) pass in the final run |
| DoD-05 | met | No open Critical or High finding (Findings); the only High as reported (CVE-2025-7962) is pre-approved PA-01 |
| DoD-06 | met | Runtime demonstration below (`phase6-runtime-demo.log`) and the 5 e2e journeys against the compose stack |
| DoD-07 | met | `trace-ac.mjs` (Traceability): every AC has tests and commits |
| DoD-08 | met (phase 7) | Root and component READMEs followed in a fresh clone: build, checks, backend 264 / frontend 80 tests, quick start (page, API, Mailpit 200) and 5 e2e tests pass (`out/logs/phase7-clean-clone.log`) |
| DoD-09 | met (phase 7) | `docs/release-notes.md`, "Must be tested manually by a human": production reCAPTCHA, real SMTP, TLS and reverse proxy (SR-08), Excel display, image scan, screen reader |
| DoD-10 | met | Every decision record is resolved or pending review (D-08, D-09); input manifest unchanged |
| DoD-11 | met | Phase 3 commits add tests, test fixtures and container files only; the freeze commit `0b8e69e` adds only the manifest, and every listed file was committed before it; test-first order per `trace-ac.mjs`; red runs in `out/logs/tdd/`; hashes match |
| DoD-12 | met | `trace-ac.mjs`: "not test-first: none"; red logs `out/logs/tdd/C01-red.log` … `C12b-red.log` |
| DoD-P01 | met | One external and one student registration answered 201 against the running stack (runtime demo §2; also E2E-01, E2E-02) |
| DoD-P02 | met | Both rows with options and consents in PostgreSQL, both JSON copies on the volume (runtime demo §3) |
| DoD-P03 | met | Mailpit holds both participant confirmations and both organizer notifications, each notification with one attachment (runtime demo §4); E2E-01 compares the attachment with the JSON copy byte for byte |
| DoD-P04 | met | Export with credentials: 200, xlsx, opened: header plus 2 rows, Slovenian text unchanged; without credentials 401; wrong password 401 (runtime demo §5) |
| DoD-P05 | met | `RecaptchaVerifierTest` exercises the production code path against a local mock verification endpoint, including a rejected token (`DoD_P05_rejectedTokenIsRejected`) |

## Runtime demonstration

`docker compose --env-file ../.env up -d --build --wait` in `02_output` (local environment), then the demonstration script; output in `out/logs/phase6-runtime-demo.log` (secrets read from `.env` into variables and never printed; the organizer address is masked).

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local compose | all containers healthy; backend uid 100, frontend uid 101 (non-root, SB-11); readiness and liveness `UP` (NFR-04); `/actuator/health` through the proxy answers the SPA page, not the actuator | pass |
| frontend | local | security headers on `/` (CSP, X-Frame-Options DENY, nosniff, no-referrer); browser journeys E2E-01..05 | pass |
| backend | local | external and student registration 201; student with an external-only option 400; rows, options, consents and JSON copies present; 4 emails in Mailpit; export 200 with credentials, 401 without and with a wrong password | pass |
| all | local | `docker compose down`, then `up`: 2 rows and 2 JSON copies still present, readiness `UP` (NFR-02) | pass |
| backend | local | container log after the demonstration: no stack trace, name, email or password | pass |

## Security controls

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `domain.RegistrationValidator`; strict request parsing in `RegistrationController` | RegistrationValidatorTest, RegistrationControllerTest, US-001/US-002 acceptance tests |
| SB-02 | `web.SecurityConfiguration`: export needs the organizer role, public endpoints listed explicitly, everything else denied | ExportSecurityIntegrationTest, ExportAcceptanceTest (AC-008-02) |
| SB-03 | BCrypt (delegating encoder); secrets only from the environment | ExportSecurityIntegrationTest (hash format), ApplicationWiringIntegrationTest; inspection of `application.properties` |
| SB-04 | HTTPS-only organizer access (SR-06); STARTTLS required by default (`MAIL_TLS=true`) | OrganizerHttpsOnlyFilterTest, AC-008-04; the TLS proxy is a manual release test |
| SB-05 | JPA bound parameters; React escaping; plain-text email; string cells | Semgrep (no injection finding), AC-006-02, Confirmation.test (markup as text), ExcelWorkbookWriterTest |
| SB-06 | `RateLimitFilter` ahead of Spring Security, failed logins counted | RateLimitFilterTest, FilterResponsesTest, ApplicationWiringIntegrationTest |
| SB-07 | `ApiExceptionHandler` generic bodies; `server.error.include-*=never`; logs carry the registration id only | RegistrationControllerTest, ApiExceptionHandlerTest, MailRegistrationNotifierTest, container log check |
| SB-08 | Dependency-Check online with the NVD key; npm audit | F-03, F-04, F-05, F-07; nothing Critical or High open |
| SB-09 | SpotBugs, PMD, Semgrep, gitleaks | F-01, F-02, F-06 |
| SB-10 | Spring Security headers with CSP; nginx headers | WebProtectionIntegrationTest; runtime demonstration |
| SB-11 | `USER app` and `USER nginx` in the images | runtime demonstration (uid 100, 101) |
| SB-12 | only the BR-01 fields are stored; fields of the other type are dropped | RegistrationValidatorTest (AC-001-03), FileJsonCopyStoreTest |
| SB-13 | purposes and retention per `security-requirements.md`; OQ-06 instruction | release notes (phase 7) |
| SB-14 | consents never preselected; consent time stored | form acceptance tests, StorageAcceptanceTest (AC-005-01) |
| SR-01 | every token verified on the backend (`RecaptchaVerifier`); test mode only by configuration | RecaptchaVerifierTest (DoD-P05), reCAPTCHA widget tests, AC-001-07, AC-002-06 |
| SR-02 | `CaptchaVerifierFactory` refuses test mode in production and empty keys | CaptchaVerifierFactoryTest |
| SR-03 | per-client rate limits and 16 KiB body limit | RateLimitFilterTest, RequestSizeLimitFilterTest, WebProtectionIntegrationTest |
| SR-04 | option ids checked against the active configuration and the type | AC-001-05, AC-002-04 tests |
| SR-05 | plain-text mail, fixed headers, validated addresses | ParticipantMailComposerTest, AC-006-02 |
| SR-06 | `OrganizerHttpsOnlyFilter`: 403 unless HTTPS or loopback | OrganizerHttpsOnlyFilterTest, FilterResponsesTest, AC-008-04 |
| SR-07 | export columns and organizer email limited to registration data | ExcelWorkbookWriterTest, OrganizerMailComposerTest; inspection |
| SR-08 | `proxy_set_header Host $proxy_host` in `frontend/nginx.conf` | inspection; the external production proxy is a manual release test |

## Traceability

Output of `node 01_input/00_general/skills/verify-release/trace-ac.mjs` (`out/logs/phase6-trace-ac.log`):

| AC | Tests | Commits | First test commit | First feat commit | TDD order |
|---|---|---|---|---|---|
| AC-001-01 | RegisterParticipantTest.java, RegistrationValidatorTest.java (2), JpaRegistrationStoreIntegrationTest.java, RegistrationControllerTest.java, e01-e02-registration.e2e.ts | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+7) | 7c236f6 | c514192 | ok |
| AC-001-02 | ExternalRegistrationAcceptanceTest.java, RegisterParticipantTest.java, RegistrationValidatorTest.java (2), RegistrationControllerTest.java | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-03 | RegisterParticipantTest.java, RegistrationValidatorTest.java | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-04 | ExternalRegistrationAcceptanceTest.java, RegistrationValidatorTest.java (3) | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-05 | ExternalRegistrationAcceptanceTest.java, RegistrationValidatorTest.java | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-06 | ExternalRegistrationAcceptanceTest.java, RegistrationValidatorTest.java | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-07 | ExternalRegistrationAcceptanceTest.java, RegisterParticipantTest.java, RegistrationValidatorTest.java (2), TestModeCaptchaVerifierTest.java (2), RegistrationControllerTest.java | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+4) | 7c236f6 | c514192 | ok |
| AC-001-08 | e01-e02-registration.e2e.ts, App.test.tsx, RegistrationForm.behaviour.test.tsx (2), RegistrationForm.test.tsx (4), externalForm.acceptance.test.tsx, api.headers.test.ts (2), api.test.ts (2), options.test.ts (2) | 4d21f37, 3b65520, f6b35a2, 7c236f6, 90084b2, 4a39b4d (+2) | 90084b2 | d0da240 | ok |
| AC-002-01 | StudentRegistrationValidatorTest.java, e01-e02-registration.e2e.ts | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-02 | StudentRegistrationAcceptanceTest.java, StudentRegistrationValidatorTest.java (2) | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-03 | StudentRegistrationAcceptanceTest.java, StudentRegistrationValidatorTest.java | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-04 | StudentRegistrationValidatorTest.java | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-05 | RegistrationValidatorTest.java | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-06 | StudentRegistrationAcceptanceTest.java, RegistrationValidatorTest.java, TestModeCaptchaVerifierTest.java | 7c4dc29, 928ecda, f02e5a4, 8e46ee6 | 7c4dc29 | 8e46ee6 | ok |
| AC-002-07 | e01-e02-registration.e2e.ts, RegistrationForm.student.test.tsx (3), studentForm.acceptance.test.tsx, options.test.ts (2) | 7c4dc29, 928ecda, 37f4caf, a983cea | 928ecda | a983cea | ok |
| AC-003-01 | ConferenceConfigurationTest.java, ConfigurationBeansTest.java, ApplicationWiringIntegrationTest.java, FormConfigControllerTest.java | 61efb2d, f8eb0a0, 0b837fa, 11f1505 | 61efb2d | 0b837fa | ok |
| AC-003-02 | ConferenceConfigurationLoaderTest.java, ConfigurationBeansTest.java | 61efb2d, f8eb0a0, 0b837fa | 61efb2d | 0b837fa | ok |
| AC-003-03 | InvalidConfigurationAcceptanceTest.java, ConferenceConfigurationTest.java, AdapterDetailsTest.java, ConferenceConfigurationLoaderTest.java | 61efb2d, f8eb0a0, 0b837fa, c93c864 | 61efb2d | 0b837fa | ok |
| AC-004-01 | e01-e02-registration.e2e.ts (2), App.test.tsx, Confirmation.test.tsx (3), RegistrationForm.behaviour.test.tsx (4), RegistrationForm.submit.test.tsx (2), confirmation.acceptance.test.tsx, api.headers.test.ts, api.submit.test.ts | 2c8cf76, 1ea6172, def629d, b5e07fa, 791bbd1, 0c962af | 2c8cf76 | def629d | ok |
| AC-004-02 | e03-field-errors.e2e.ts, RegistrationForm.behaviour.test.tsx, RegistrationForm.submit.test.tsx, confirmation.acceptance.test.tsx, api.submit.test.ts | 2c8cf76, 1ea6172, def629d, b5e07fa, 791bbd1, 0c962af | 2c8cf76 | def629d | ok |
| AC-004-03 | e03-field-errors.e2e.ts, RegistrationForm.behaviour.test.tsx (3), RegistrationForm.submit.test.tsx (2), confirmation.acceptance.test.tsx, validation.messages.test.ts (3), validation.test.ts (7) | 2c8cf76, 1ea6172, def629d, b5e07fa, 791bbd1, 7b960dc (+1) | 2c8cf76 | def629d | ok |
| AC-004-04 | ApiExceptionHandlerTest.java, App.test.tsx, RegistrationForm.behaviour.test.tsx (2), RegistrationForm.submit.test.tsx (2), confirmation.acceptance.test.tsx (2), api.headers.test.ts (2), api.submit.test.ts (4) | 2c8cf76, 1ea6172, def629d, b5e07fa, 791bbd1, 7b960dc (+1) | 2c8cf76 | def629d | ok |
| AC-005-01 | ConsentEmbeddableTest.java, JpaRegistrationStoreIntegrationTest.java, e01-e02-registration.e2e.ts | e75edee, 4e2374a, 3fabee8 | e75edee | 3fabee8 | ok |
| AC-005-02 | RegisterParticipantTest.java, RegistrationTransactionIntegrationTest.java, ConfigurationBeansTest.java, FileJsonCopyStoreTest.java, e01-e02-registration.e2e.ts | e75edee, 4e2374a, 3fabee8 | e75edee | 3fabee8 | ok |
| AC-005-03 | RegisterParticipantTest.java, RegistrationTransactionIntegrationTest.java, FileJsonCopyStoreTest.java, JpaRegistrationFlushIntegrationTest.java, RegistrationControllerTest.java | e75edee, 4e2374a, 3fabee8, c93c864 | e75edee | 3fabee8 | ok |
| AC-005-04 | FileJsonCopyStoreTest.java, JpaRegistrationStoreIntegrationTest.java, e04-slovenian-characters.e2e.ts | e75edee, 4e2374a, 3fabee8 | e75edee | 3fabee8 | ok |
| AC-006-01 | RegistrationNotificationIntegrationTest.java, MailBeansTest.java, MailRegistrationNotifierTest.java, ParticipantMailComposerTest.java, e01-e02-registration.e2e.ts | 7974452, 01f3f83, 80bfd6e | 7974452 | 80bfd6e | ok |
| AC-006-02 | ParticipantMailComposerTest.java | 7974452, 01f3f83, 80bfd6e | 7974452 | 80bfd6e | ok |
| AC-006-03 | RegisterParticipantTest.java, RegistrationNotificationIntegrationTest.java, MailRegistrationNotifierTest.java | 7974452, 01f3f83, 80bfd6e | 7974452 | 80bfd6e | ok |
| AC-007-01 | MailBeansTest.java, MailRegistrationNotifierTest.java, OrganizerMailComposerTest.java, OrganizerMailDetailsTest.java, e01-e02-registration.e2e.ts | 0f19632, 7997e79, 68bc1de, 0b109c2, 11f1505, c93c864 | 0f19632 | 0b109c2 | ok |
| AC-007-02 | OrganizerMailComposerTest.java, e01-e02-registration.e2e.ts | 0f19632, 68bc1de, 0b109c2 | 0f19632 | 0b109c2 | ok |
| AC-008-01 | ExportRegistrationsTest.java, ConfigurationBeansTest.java, ExcelWorkbookStyleTest.java, ExcelWorkbookWriterTest.java, JpaRegistrationStoreIntegrationTest.java, ExportControllerTest.java, ExportSecurityIntegrationTest.java, e05-export.e2e.ts | f6b35a2, 47f0a6c, 4a39b4d, 975b4d0, d73ae1a | 47f0a6c | d73ae1a | ok |
| AC-008-02 | ApplicationWiringIntegrationTest.java, ExportSecurityIntegrationTest.java, e05-export.e2e.ts | f6b35a2, 47f0a6c, 4a39b4d, 58055e1, 575246e, e77bc3c | 47f0a6c | 575246e | ok |
| AC-008-03 | ExcelWorkbookWriterTest.java, e04-slovenian-characters.e2e.ts | f6b35a2, 47f0a6c, 4a39b4d, 975b4d0, d73ae1a | 47f0a6c | d73ae1a | ok |
| AC-008-04 | ExportSecurityIntegrationTest.java, FilterResponsesTest.java (2), OrganizerHttpsOnlyFilterTest.java (2) | f6b35a2, 47f0a6c, 4a39b4d, 0b8e69e, 58055e1, 575246e | 47f0a6c | 575246e | ok |

35 acceptance criteria; without a test: none; without a commit: none; not test-first: none.

The tool reads only hyphenated ids, so the frozen backend acceptance classes (method names `AC_001_03_…`) are listed only where their parameterised display names carry the id; every criterion also has its frozen test (`docs/03_test-strategy.md`, "Acceptance criteria → frozen tests").

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | gitleaks v8.30.1 (`phase6-gitleaks.json`) | 32 `generic-api-key` matches, all in committed test logs written before cycle C07b: Spring Boot's own "Using generated security password" line for its default in-memory user. The values are random per test JVM and never a project secret; the `.env` leak check finds no value in any file or commit message | accepted: no secret is involved, and the line no longer appears since the organizer account exists (C07b) |
| F-02 | Medium | Semgrep 1.177.0 (`phase6-semgrep.json`) | `frontend/.npmrc` sets no `min-release-age` (rule severity WARNING → Medium) | accepted: the setting needs npm 11.10, the pinned npm is 11.6.2 (changing it needs approval); every dependency is pinned exactly and installed from the committed lock file with `npm ci` |
| F-03 | Medium | npm audit (`phase6-npm-audit.json`) | 2 Moderate advisories: `qs` 6.15.1 via `typed-rest-client` via `@stryker-mutator/core`; `npm audit --omit=dev`: 0 | accepted: PA-03, D-02 (dev tooling, not in the bundle) |
| F-04 | Low | Dependency-Check 12.1.0, online (`phase6-dependency-check-report.json`) | CVE-2025-7962 on `angus-activation` 2.0.3 (CVSS 7.5, reported High) | false positive per PA-01, D-05 (shipped `angus-mail` is 2.0.5); suppressed for that artifact only; 0 open |
| F-05 | Low | Dependency-Check 12.1.0 | CVE-2025-15104 on `hibernate-validator` 9.1.3.Final (CVSS 5.3) | false positive per PA-02, D-05 (CPE name collision); suppressed for that artifact only |
| F-06 | Low | SpotBugs 4.10.4.1 | EI_EXPOSE_REP / EI_EXPOSE_REP2 on records and injected collaborators | accepted: D-11; only these two patterns are excluded, every other pattern is active (0 bugs) |
| F-07 | Medium | inspection (SB-08) | the container base images (postgres, eclipse-temurin, node, nginx, mailpit) are not scanned, because `tech-stack.md` lists no image scanner | accepted: the images are the pinned versions of `tech-stack.md`; an image scan is listed for the operator in the release notes |

Semgrep reported no "basic authentication" finding on the organizer endpoint, so PA-04 was not needed. No Critical or High finding is open.

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| none | – | no Critical or High finding was found; Medium and Low findings are triaged above | – |
