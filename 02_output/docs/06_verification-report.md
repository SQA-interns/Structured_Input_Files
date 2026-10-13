# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-13 against commit range `6bf4a55..HEAD`. Raw outputs are in `out/logs/06_*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | Pass | Final run: backend 216/216 (unit 127, acceptance 84, architecture 5), frontend 69/69 (unit 59, acceptance 10), e2e 5/5 (`logs/06_backend-final.log`, `06_frontend-final.log`, `06_e2e-final.log`). Both manifests recomputed after LF normalisation: 39/39 input files and 34/34 frozen test files match; no commit touched a frozen file after the freeze `a3dbbb7`. |
| DoD-02 | Pass | Backend `spotless:check pmd:check pmd:cpd-check spotbugs:check` BUILD SUCCESS (SpotBugs Medium threshold, D-13); frontend `prettier --check`, `eslint`, `tsc --noEmit` clean; Semgrep 1 Medium accepted (F-02) |
| DoD-03 | Pass | Quality gate met for both components (`03_test-strategy.md`, "Quality gate"): backend line 99.32 %, mutation 95.72 % (PIT, 0 rounds); frontend line 100 %, mutation 94.83 % (Stryker, 1 round + confirming full run). No code or test changed since, so no re-measurement. |
| DoD-04 | Pass | ArchUnit `ArchitectureTest` (5 rules: plain-Java domain, declared package dependencies of spec 2, nothing depends on `web`, ports for side effects, no slice cycles) passes; AR-01 (`/api` only) by code inspection of `frontend/src/api.ts` and the nginx proxy; AR-04 by AC-003-02; AR-05 by AC-005-03; AR-06 by `ddl-auto=validate` + Flyway V1; AR-07: bundle contains no secret (site key only via `/api/form-config`) |
| DoD-05 | Pass | No open Critical or High (Findings below) |
| DoD-06 | Pass | Runtime demonstration below (`logs/06_runtime-demo.log`) and e2e 5/5 on the rebuilt images |
| DoD-07 | Pass | Traceability table below: every AC has at least one test and one commit |
| DoD-08 | Pass (phase 7) | Fresh clone of `43ab469` followed by the READMEs: backend build, frontend `npm ci`, build, 69 tests, check, `docker compose up --build --wait` (all healthy), form config 200, e2e 5/5 from the clone (`logs/07_clean-clone.log`). On Windows a deep clone path needs `core.longpaths` (now in the root README). |
| DoD-09 | Pass (phase 7) | `release-notes.md`, "Must be tested manually by a human": reCAPTCHA with production keys, SMTP delivery, TLS and proxy, HTTPS-only export, production start, backup and retention |
| DoD-10 | Pass | D-01 … D-14 each have a resolution or "pending review" (D-08 … D-11); manifests unchanged |
| DoD-11 | Pass with divergences | Phase 3 commits add no production code (only tests, harness, build/compose files); the freeze commit `a3dbbb7` adds only the manifest and every listed file was committed earlier; every story has `feat` commits naming its ID. Divergences recorded in `run-log.json`: three test commits over 400 lines (`b7b37af` 476, `7df20af` 445, `3ca6b52` 431), two subjects over 72 characters (`63b0068` 73, `14f0291` 75), phase 0 bootstrap committed in a burst. |
| DoD-P01 | Pass | One external and one student registration through the frontend proxy: 201 each (`06_runtime-demo.log`, repeat section), plus E2E-01/02 |
| DoD-P02 | Pass | Both rows in PostgreSQL (`psql`), JSON copies 711 and 792 bytes in `/data/json-copies` |
| DoD-P03 | Pass | Mailpit: participant confirmation to each participant, organizer notification with 1 attachment (organizer address redacted in the log) |
| DoD-P04 | Pass | Export with credentials 200, xlsx, ZIP signature `PK`; without credentials 401; wrong password 401 |
| DoD-P05 | Pass | `RecaptchaProductionPathAcceptanceTest` (5 tests) against a mocked siteverify endpoint, including rejected tokens; `RecaptchaVerifierTest` (6 tests) |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local (`docker compose up -d --wait`, images rebuilt from the final code) | `docker compose down` then `up`: 16 rows and 16 JSON copies before and after (NFR-02) | Pass |
| backend | local | `/actuator/health`, `/liveness`, `/readiness` 200 UP; other actuator endpoints refused; compose health checks healthy for all four services (NFR-04) | Pass |
| backend + frontend | local | registration external + student (201), rows, copies, emails, export 200/401/401 (DoD-P01 … P04) | Pass (first attempt in the log answered 400 because the demo script passed UTF-8 bodies as Windows command-line arguments; repeated with bodies from files) |
| frontend | local | headers: CSP, `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy` on the page and the API (SB-10) | Pass |
| backend + frontend | local | 17 KB body: 413 at nginx and at the backend; 22 registration requests in one minute: 429 from the 21st request of the client (earlier demo requests counted) (SR-03) | Pass |
| frontend | local | request with `Host: evil.example` answered normally; nginx sets `Host backend` (SR-08) | Pass |
| backend | `prod` profile with test mode on, database reachable | refused to start: "RECAPTCHA_TEST_MODE is on; RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY must be set" (SR-02) | Pass |
| backend + frontend | local | processes run as `app` and `nginx` (SB-11) | Pass |
| frontend + backend | local | Playwright E2E-01 … E2E-05 | 5/5 |

## Security controls

| ID | Where implemented | How checked |
|---|---|---|
| SB-01 | `RegistrationValidator`, unknown JSON properties rejected, `BodySizeLimitFilter` | AC-001-02..06, AC-002-02..05 tests, `RegistrationValidatorTest` |
| SB-02 | `SecurityConfiguration`: only `GET /api/form-config`, `POST /api/registrations`, health public; export needs role ORGANIZER; everything else denied | AC-008-02, runtime (actuator 401) |
| SB-03 | organizer password BCrypt-hashed at start; secrets only from env, no defaults | `SecuritySettingsTest`, inspection of `application.properties` |
| SB-04 | TLS at the external proxy (production), plain HTTP only on 127.0.0.1 | inspection of `docker-compose.yml` (127.0.0.1 bindings); manual test listed for phase 7 |
| SB-05 | JPA parameter binding; React output encoding; plain-text emails; string cells | AC-006-02, `RegistrationExportTest` (formula text stays a string), Semgrep clean |
| SB-06 | `RateLimitFilter` per client and endpoint | `RequestFiltersTest`, runtime 429 |
| SB-07 | `ApiExceptionHandler`, `ErrorResponses`, logs with ids only | AC-004-04, `WebLayerTest`, log inspection (below) |
| SB-08 | Dependency-Check, npm audit | 0 open (PA-01/02 suppressed, PA-03 accepted) |
| SB-09 | Semgrep, gitleaks | F-01, F-02 |
| SB-10 | Spring Security headers, nginx headers | runtime headers |
| SB-11 | non-root users in both images | runtime `id -un` |
| SB-12, SB-13 | only the fields of `business-rules.md`; purposes and retention in `security-requirements.md`, OQ-06 in release notes | inspection of the schema contract |
| SB-14 | consents unchecked, required explicitly, stored with time | AC-001-08, AC-002-07, AC-005-01 |
| SR-01 | backend verification `RecaptchaVerifier` | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | `ProductionStartGuard` | `SecuritySettingsTest`, runtime refusal |
| SR-03 | rate limits, body limit (backend and nginx) | `RequestFiltersTest`, runtime 413/429 |
| SR-04 | option ids checked against active options per type | AC-001-05, AC-002-04 |
| SR-05 | plain text bodies, fixed subjects, single bare recipient | AC-006-02, `MailNotifierTest` |
| SR-06 | `HttpsOnlyFilter` before authentication | AC-008-04, `RequestFiltersTest` |
| SR-07 | export and organizer mail contain only registration data | AC-007-01, AC-008-01, `RegistrationExportTest` |
| SR-08 | nginx `proxy_set_header Host backend`, `X-Forwarded-For $remote_addr` | inspection, runtime |

Logs and error responses: backend logs during tests and the demonstration contain registration ids and exception class names only; error bodies contain only the messages of spec 4.3 (checked by AC-004-04, `WebLayerTest`). Secret-leak check (skill step 8): no tracked or untracked file and no commit message contains a `.env` value (both commands printed nothing / `0`).

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | ExternalRegistrationAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-02 | ExternalRegistrationAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-03 | ExternalRegistrationAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-04 | ExternalRegistrationAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-05 | ExternalRegistrationAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-06 | ExternalRegistrationAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-07 | ExternalRegistrationAcceptanceTest.java, RecaptchaProductionPathAcceptanceTest.java | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b11bbe, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-001-08 | E2E-01-external-registration.spec.ts, external-form.acceptance.test.tsx | tests: bdc4189, f89e779, bdbe036, 3ca6b52, e7b9cb4; code: 8ec68e0, 3b27797, 361f095, 34cf8a2, 4f08396, 19b939b |
| AC-002-01 | StudentRegistrationAcceptanceTest.java, E2E-02-student-registration.spec.ts | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-02 | StudentRegistrationAcceptanceTest.java | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-03 | StudentRegistrationAcceptanceTest.java | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-04 | StudentRegistrationAcceptanceTest.java | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-05 | StudentRegistrationAcceptanceTest.java | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-06 | RecaptchaProductionPathAcceptanceTest.java, StudentRegistrationAcceptanceTest.java | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-002-07 | E2E-02-student-registration.spec.ts, student-form.acceptance.test.tsx | tests: c7bf5b1, fa1cffa, bdbe036; code: 8ec68e0, 4f08396, 19b939b |
| AC-003-01 | FormConfigAcceptanceTest.java | tests: 54ae7b3, c797de6; code: 4afbd4f, 36cee7b |
| AC-003-02 | ChangedConfigurationAcceptanceTest.java | tests: 54ae7b3, c797de6; code: 4afbd4f, 36cee7b |
| AC-003-03 | InvalidConfigurationAcceptanceTest.java | tests: 54ae7b3, c797de6; code: 4afbd4f, 36cee7b |
| AC-004-01 | E2E-01-external-registration.spec.ts, E2E-02-student-registration.spec.ts, confirmation.acceptance.test.tsx | tests: b7dbab9, 3ca6b52, e7b9cb4; code: a773e19 |
| AC-004-02 | E2E-03-field-errors.spec.ts, confirmation.acceptance.test.tsx | tests: b7dbab9, 3ca6b52, e7b9cb4; code: a773e19 |
| AC-004-03 | E2E-03-field-errors.spec.ts, confirmation.acceptance.test.tsx | tests: b7dbab9, 3ca6b52, e7b9cb4; code: a773e19 |
| AC-004-04 | confirmation.acceptance.test.tsx | tests: b7dbab9, 14f0291, 3ca6b52, e7b9cb4; code: a773e19 |
| AC-005-01 | StorageAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: bd6cbc9, 119e907, 0f8b50b; code: 5132928, 3b11bbe |
| AC-005-02 | StorageAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: bd6cbc9, 119e907, 0f8b50b; code: 5132928, 3b11bbe |
| AC-005-03 | JsonCopyFailureAcceptanceTest.java | tests: bd6cbc9, 119e907, 0f8b50b; code: 5132928, 3b11bbe |
| AC-005-04 | StorageAcceptanceTest.java, E2E-04-slovenian-characters.spec.ts | tests: bd6cbc9, 119e907, 0f8b50b; code: 5132928, 3b11bbe |
| AC-006-01 | ParticipantEmailAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: 2a03f59, ed2df9e; code: 8275a61 |
| AC-006-02 | ParticipantEmailAcceptanceTest.java | tests: 2a03f59, ed2df9e; code: 8275a61 |
| AC-006-03 | MailServerDownAcceptanceTest.java | tests: 2a03f59, ed2df9e; code: 8275a61 |
| AC-007-01 | OrganizerNotificationAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: 63b0068, ed2df9e; code: 8275a61, 46f7c21 |
| AC-007-02 | OrganizerNotificationAcceptanceTest.java, E2E-01-external-registration.spec.ts | tests: 63b0068, ed2df9e; code: 8275a61, 46f7c21 |
| AC-008-01 | ExportAcceptanceTest.java, E2E-05-organizer-export.spec.ts | tests: a6e35b2, 3f04868, 7df20af; code: 59e0961, 073cebb |
| AC-008-02 | ExportAcceptanceTest.java, E2E-05-organizer-export.spec.ts | tests: a6e35b2, 3f04868, 7df20af; code: 59e0961, 073cebb |
| AC-008-03 | ExportAcceptanceTest.java, E2E-04-slovenian-characters.spec.ts | tests: a6e35b2, 3f04868, 7df20af; code: 59e0961, 073cebb |
| AC-008-04 | ExportAcceptanceTest.java | tests: a6e35b2, 3f04868, 7df20af; code: 59e0961, 073cebb |

Test files: backend `src/test/java/si/konferenca/registration/acceptance/*`, frontend `tests/acceptance/*`, `e2e/*`; unit tests per area are listed in `03_test-strategy.md`.

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | gitleaks v8.30.1 (`logs/06_gitleaks.json`) | 8 `generic-api-key` matches in `logs/03_backend-acceptance-first-run.log`: Spring Security's random "generated security password" printed by the skeleton's short-lived test JVMs in phase 3 | Accepted: false positive, not a project secret, no system uses it; history is not rewritten |
| F-02 | Medium | Semgrep 1.177.0 (`logs/06_semgrep.json`) | `npm-missing-minimum-release-age` on `frontend/.npmrc` | Accepted: the setting needs npm ≥ 11.10, the pinned npm is 11.6.2; every dependency is pinned exactly with a committed lock file and installed with `npm ci` |
| F-03 | Low (tool: High) | Dependency-Check 12.1.0 | CVE-2025-7962 on `angus-activation` 2.0.3 | PA-01, D-03: suppressed for that artifact; `angus-mail` 2.0.5 ships |
| F-04 | Low (tool: Medium) | Dependency-Check | CVE-2025-15104 on `hibernate-validator` 9.1.3 | PA-02, D-04: CPE name collision, suppressed |
| F-05 | Medium | npm audit | 2 moderate advisories on `qs` via `@stryker-mutator/core` (dev only; `--omit=dev` 0) | PA-03, D-05: accepted |
| F-06 | Low | SpotBugs 4.10.4.1 | 3 EI_EXPOSE_REP2 on constructor-injected Spring beans | D-13: false positive, excluded for those two classes |
| F-07 | Low | runtime demonstration | the production start guard runs after Flyway connects, so with an unreachable database the start fails on the database first | Accepted: production still refuses to start; with the database reachable the guard refuses (demonstrated) |
| F-08 | Low | PIT, Stryker | 16 backend and 21 frontend surviving mutants, classified in `03_test-strategy.md` | Accepted: gate met, stop rule applies |
| F-09 | Low | process (runtime demonstration) | the demo script printed the organizer recipient address from `.env` (`ORGANIZER_EMAILS`) to the session output and the demo log | Fixed: the address was redacted from `logs/06_runtime-demo.log` before commit; leak check clean; the mail went only to the local Mailpit |

Counts as found: Critical 0, High 1 (F-03 as reported by the tool), Medium 3 (F-02, F-04 as reported, F-05), Low 5. Open after triage: Critical 0, High 0, Medium 0 (F-02 and F-05 accepted with a reason).

Defects found and fixed before phase 6 (not phase 6 findings): attachment line endings (E2E-01, phase 4, commit `fix: keep JSON attachment byte-equal to stored copy`); reCAPTCHA `"success": "true"` accepted and display-name recipient rewritten (unit tests, phase 5, two `fix` commits).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| — | none: no Critical or High finding needed a fix in phase 6 | — | — |
