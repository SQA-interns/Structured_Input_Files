# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-12 at the HEAD of `kyuhi/single-agent/sdd/v005` after phase 5. Logs: `out/logs/06_*`.

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | met | Full suite green: backend 241/0 (138 unit, 5 integration, 92 acceptance, 6 architecture; `05_backend-coverage-all.log`), frontend 52/0 (42 unit, 10 acceptance; `06_final-checks.log`), e2e 5/0 (`06_runtime-demo.log`). Frozen hashes: `06_hash-check.log` — 30/30 match, 0 frozen files changed since freeze commit `2c15ad4`. |
| DoD-02 | met | `06_final-checks.log`: Spotless (google-java-format), PMD, CPD, SpotBugs clean (exposure patterns excluded by D-14); Prettier, ESLint, `tsc --noEmit`, Vite build clean. Semgrep: 2 findings triaged (F-02, F-03). |
| DoD-03 | met | Quality gate met at the first mutation run, 0 survivor rounds: backend line 99.24 % (all levels), PIT raw = adjusted 91.44 % (267/292); frontend line 100 %, Stryker raw = adjusted 90.24 % (370/410; 90.02 % counting the compile-error mutant). Scope, levels, tools: `docs/03_test-strategy.md` "Test measures"; mutation tests exclude e2e (D-16). |
| DoD-04 | met | `ArchitectureTest` (6 rules: layers of specification section 2 as amended by D-15, domain/config independence, no slice cycles AR-03, controllers in `web`, entities in `domain`, no repositories in `web`) passes. |
| DoD-05 | met | No open Critical/High: Dependency-Check 0 open (49 suppressed per PA-01, PA-02, D-07, D-08), `npm audit --omit=dev` 0, Semgrep no ERROR, Gitleaks only false positives (F-01). |
| DoD-06 | met | `06_runtime-demo.log`: stack built from HEAD with `docker compose`, all four containers healthy, the five core flows run through the browser (E2E-01…05), health/readiness UP, data present after `down`/`up`. |
| DoD-07 | met | Traceability table below: all 35 AC have tests and a feature commit. |
| DoD-08 | met (phase 7) | `out/logs/07_clean-checkout.log`: fresh clone (with `core.longpaths`), backend package, check and 241 tests, frontend `npm ci`, check, 52 tests and build, `docker compose up --build` from the clone (all healthy, page 200) and the 5 e2e tests against that stack. |
| DoD-09 | met (phase 7) | `docs/release-notes.md` "Must be tested manually by a human": reCAPTCHA with production keys, real mailbox delivery, TLS and proxy routing, production startup guard, workbook in Excel, browser and keyboard check, backups. |
| DoD-10 | met | All 16 decisions have a resolution (D-10, D-11, D-12 pending review by design). Input manifest 38/38 match; no input file changed since the start commit. |
| DoD-11 | met with one divergence | Phase 3 commits contain tests, test resources and the D-13 deployment harness only (no file under `src/main`); the freeze commit `2c15ad4` adds only the manifest, and every listed file was committed earlier. Every story has a feature commit naming its US/AC ids. One commit (`a43d21f`, US-003) exceeds the 400-line guide without a stated reason (F-08). Hashes match. |
| DoD-P01 | met | E2E-01 and E2E-02 pass against the stack; 6 external and 3 student registrations stored during the demonstration. |
| DoD-P02 | met | `select type, count(*)`: EXTERNAL 6, STUDENT 3; 9 JSON copies; 0 rows without copy. |
| DoD-P03 | met | Mailpit: 9 participant confirmations, 9 organizer notifications, all 9 with attachment; E2E-01 compares attachment bytes with the copy. |
| DoD-P04 | met | Export with credentials 200, xlsx content type, `PK` signature, rows = registrations (E2E-05); without credentials 401, wrong password 401. |
| DoD-P05 | met | `RecaptchaProductionVerifierAcceptanceTest` (5 tests, production verifier against a local stub `siteverify`, accepted and rejected tokens) and `RecaptchaVerifierTest` (9 unit tests incl. non-200, unparsable, unreachable). |

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, frontend, PostgreSQL, Mailpit | local (`docker compose --env-file ../.env up -d --build`) | health, liveness, readiness | `UP` (NFR-04); containers use them in health checks |
| frontend + backend | local | E2E-01 external, E2E-02 student, E2E-03 field errors, E2E-04 Slovenian characters, E2E-05 export | 5 passed |
| backend | local | storage, JSON copies, Mailpit, export with/without/wrong credentials | see DoD-P02 … P04 |
| backend | local | `docker compose down` then `up` | 9 rows and 9 copies before and after (NFR-02) |
| backend, frontend | local | process user | backend uid 10001, frontend uid 101 (nginx), non-root (SB-11) |
| backend via frontend nginx | local | 20 kB POST; 70 form-config requests in a minute; spoofed `Host` header | 413; 58 × 200 then 12 × 429; served normally, nginx sets the upstream Host itself |

## Security baseline and project requirements

| Item | Where implemented | How checked |
|---|---|---|
| SB-01 | `RegistrationValidator`, `ConferenceConfigurationParser` | acceptance AC-001-02…06, AC-002-02…05; 27 validator unit tests |
| SB-02 | `SecurityConfiguration` (export needs organizer role, everything unlisted denied) | AC-008-02; `SecurityIntegrationTest` |
| SB-03 | BCrypt hash of `ORGANIZER_PASSWORD` in `InMemoryUserDetailsManager`; secrets only from `.env` | `SecurityIntegrationTest` (stored value is `$2…`); secret-leak check |
| SB-04 | TLS by the external reverse proxy (environments.md); HTTPS-only export (SR-06) | inspection; manual test in release notes |
| SB-05 | JPA parameterised queries; plain-text emails; React text rendering; xlsx string cells | Semgrep; `MailNotifierTest`, `ExcelWriterTest` (formula-like text stays text) |
| SB-06 | `RateLimitFilter` (60/10/10 per minute per client) | `RequestFiltersTest`; runtime 429 |
| SB-07 | `ApiExceptionHandler` generic messages; logs carry ids and exception types only | `ApiExceptionHandlerTest`; AC-004-04; log inspection below |
| SB-08 | Dependency-Check, npm audit | phase 0 and phase 6 scans |
| SB-09 | Semgrep, SpotBugs, PMD, Gitleaks | phase 6 scans |
| SB-10 | backend headers (`SecurityConfiguration`), frontend `nginx.conf` | `SecurityIntegrationTest`; runtime headers |
| SB-11 | non-root users in both Dockerfiles | runtime `id -u` |
| SB-12, SB-13 | only BR-01 fields, options, consents; retention per OQ-06 (release notes) | specification section 10; inspection |
| SB-14 | consents unchecked; stored with wording and time | AC-001-08, AC-002-07, AC-005-01 |
| SR-01 | backend verification in `RegistrationService` before storing | AC-001-07, AC-002-06, DoD-P05 |
| SR-02 | `StartupGuard` (test mode only in `local`/`test`, keys required otherwise) | `StartupGuardTest` |
| SR-03 | `RateLimitFilter`, `BodySizeLimitFilter`, nginx `client_max_body_size` | unit tests; runtime 413/429 |
| SR-04 | options checked against the active configured set and type | AC-001-05, AC-002-04 |
| SR-05 | user input only in plain-text bodies; validated address in To | AC-006-02; `MailNotifierTest` |
| SR-06 | `HttpsOnlyOrganizerFilter` (403 before authentication, D-10) | AC-008-04; `RequestFiltersTest` |
| SR-07 | export columns and JSON copy contain registration data only | AC-007-02, AC-008-01; `ExcelWriterTest`, `JsonCopyStoreTest` |
| SR-08 | frontend `nginx.conf` sets `proxy_set_header Host backend`; the production example in `backend/README.md` (phase 7) does the same | inspection; runtime spoofed-Host request |

Logs and error responses: the backend logs registration ids, exception class names and reCAPTCHA status codes only; no names, emails, tokens or secrets appear in `out/logs/` apart from test fixtures. Error bodies seen in tests and at runtime contain only the generic messages of `ApiExceptionHandler`. Secret-leak check (`verify-release` step 8): no file and 0 commit messages contain a `.env` value.

## Traceability

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | `ExternalRegistrationAcceptanceTest` (2), E2E-01 | `b50bc6a`, `714db96` |
| AC-001-02 | `ExternalRegistrationAcceptanceTest` (12 cases) | `714db96` |
| AC-001-03 | `ExternalRegistrationAcceptanceTest` | `714db96` |
| AC-001-04 | `ExternalRegistrationAcceptanceTest` (7 cases) | `714db96` |
| AC-001-05 | `ExternalRegistrationAcceptanceTest` (3 cases) | `714db96` |
| AC-001-06 | `ExternalRegistrationAcceptanceTest` (2) | `714db96` |
| AC-001-07 | `ExternalRegistrationAcceptanceTest` (2), `RecaptchaProductionVerifierAcceptanceTest` (3) | `70875f9`, `714db96` |
| AC-001-08 | `external-form.acceptance.test.tsx`, E2E-01 | `d586e4b`, `992633a`, `1416c2a` |
| AC-002-01 | `StudentRegistrationAcceptanceTest`, E2E-02 | `714db96` |
| AC-002-02 | `StudentRegistrationAcceptanceTest` (18 cases) | `714db96` |
| AC-002-03 | `StudentRegistrationAcceptanceTest` (4 cases) | `714db96` |
| AC-002-04 | `StudentRegistrationAcceptanceTest` (3) | `714db96` |
| AC-002-05 | `StudentRegistrationAcceptanceTest` | `714db96` |
| AC-002-06 | `StudentRegistrationAcceptanceTest` (2), `RecaptchaProductionVerifierAcceptanceTest` (2) | `70875f9`, `714db96` |
| AC-002-07 | `student-form.acceptance.test.tsx` (2), E2E-02 | `d7f1afc`, `992633a` |
| AC-003-01 | `ConferenceOptionsAcceptanceTest` | `a43d21f` |
| AC-003-02 | `ConferenceOptionsReconfigurationAcceptanceTest` (2) | `a43d21f`, `714db96` |
| AC-003-03 | `InvalidConferenceOptionsAcceptanceTest` (3 cases) | `a43d21f` |
| AC-004-01 | `confirmation.acceptance.test.tsx`, E2E-01, E2E-02 | `1416c2a` |
| AC-004-02 | `confirmation.acceptance.test.tsx`, E2E-03 | `992633a` |
| AC-004-03 | `confirmation.acceptance.test.tsx` (3), E2E-03 | `992633a` |
| AC-004-04 | `confirmation.acceptance.test.tsx` (2) | `992633a` |
| AC-005-01 | `RegistrationStorageAcceptanceTest` (2), E2E-01 | `b50bc6a`, `bee032d` |
| AC-005-02 | `RegistrationStorageAcceptanceTest`, E2E-01 | `bee032d` |
| AC-005-03 | `JsonCopyFailureAcceptanceTest` (2) | `bee032d` |
| AC-005-04 | `RegistrationStorageAcceptanceTest`, E2E-04 | `bee032d` |
| AC-006-01 | `ParticipantEmailAcceptanceTest` (2), E2E-01 | `afc079d` |
| AC-006-02 | `ParticipantEmailAcceptanceTest` | `afc079d`, `ba57419` |
| AC-006-03 | `MailServerUnavailableAcceptanceTest` (2) | `afc079d` |
| AC-007-01 | `OrganizerNotificationAcceptanceTest` (2), E2E-01 | `ba57419` |
| AC-007-02 | `OrganizerNotificationAcceptanceTest`, E2E-01 | `ba57419`, `5b9e399` |
| AC-008-01 | `RegistrationExportAcceptanceTest` (2), E2E-05 | `8ab624d` |
| AC-008-02 | `RegistrationExportAcceptanceTest` (3), E2E-05 | `8ab624d` |
| AC-008-03 | `RegistrationExportAcceptanceTest`, E2E-04 | `8ab624d` |
| AC-008-04 | `RegistrationExportAcceptanceTest` (3) | `8ab624d` |

## Findings

Severity per `general/quality/severity-scale.md`; tool levels mapped by its table.

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | Gitleaks v8.30.1 (history and files) | 16 `generic-api-key` matches: 15 are Spring Boot's random per-start development passwords printed by the phase 3 skeleton (`logs/03_backend-acceptance-run.log`), 1 is the acceptance-test organizer password constant in `TestStack.java`. No `.env` value, no credential of any environment. | accepted: false positive (theoretical) |
| F-02 | Medium | Semgrep `npm-missing-minimum-release-age` (MEDIUM) | `frontend/.npmrc` sets no minimum release age for newly published packages. | accepted: every dependency is pinned exactly, the lock file is committed and images use `npm ci`, so no unreviewed version is resolved; the pins are older than the suggested age |
| F-03 | Low | Semgrep `missing-internal` (WARNING → Medium) | `nginx.conf` `/api/` location uses `proxy_pass` without `internal`. | lowered to Low: the location must be public (it is the API route), the upstream is a constant set in the file, not user input |
| F-04 | Low | runtime inspection | API responses through the frontend nginx carry the security headers twice (backend and nginx); browsers apply the stricter combination. | accepted (cosmetic) |
| F-05 | Low | Dependency-Check, phase 0 (D-08) | CVEs in `httpclient5`/`httpcore5` shaded into test-scope `docker-java-transport-zerodep` 3.7.1. | accepted by the human as Low (D-08): test scope only, not shipped |
| F-06 | Medium | npm audit (PA-03, D-04) | Moderate `qs` advisories via `@stryker-mutator/core`. | accepted (PA-03): dev tooling, `npm audit --omit=dev` 0 |
| F-07 | Low | Dependency-Check (PA-01, PA-02, D-07) | CPE false positives on `angus-activation`, `hibernate-validator`, `testcontainers-postgresql`. | suppressed per artifact (D-02, D-03, D-07) |
| F-08 | Low | process (DoD-11) | Commit `a43d21f` exceeds the commit size guide without a reason in its message. | accepted; recorded as divergence in `run-log.json`; history is not rewritten |

No Critical or High finding is open. No finding needed a fix after phase 5, so no verify → fix → re-verify loop ran, and coverage and mutation figures were not re-measured (no code or test changed since phase 5).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| — | none | — | — |
