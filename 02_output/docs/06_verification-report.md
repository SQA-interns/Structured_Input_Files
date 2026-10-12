# Verification report

> Written in: phase 6 · Source: `general/quality/*`, `general/security/*`, `project/02_design/*` · Procedure: `general/skills/verify-release` · Agent: writes

This is a self-check by the development agent, not an independent review.

Verified on 2026-10-12 at commit range `e74fb75..HEAD`. Raw output is in `out/logs/phase6-*` (and `out/logs/phase5-full-run-*` for the full suite).

## Summary

| Item | Result |
|---|---|
| Manifests (`00_input-manifest.sha256`, `03_acceptance-manifest.sha256`) | 35/35 and 31/31 hashes match (`logs/phase6-manifest-check.log`) |
| Full suite | backend 139/139 (`./mvnw -B verify`), frontend 34/34 (`npx vitest run`), e2e 4/4 against the compose stack |
| Checks (ES-05) | backend `compile spotless:check pmd:check pmd:cpd-check spotbugs:check` clean; frontend `npm run check` (Prettier, ESLint, `tsc --noEmit`) clean |
| Runtime demonstration | all flows pass (`logs/phase6-runtime-demo.log`) |
| `.env` leak check | files: none listed; commit messages: `0` |
| Open Critical/High | none (F-06 lowered to Low by the human, D-26) |

## Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-01 | pass | `logs/phase5-full-run-backend.log` (139 passed), `logs/phase5-full-run-frontend.log` (32 passed), `logs/phase6-e2e-after-f04.log` (4 passed); frozen hashes 31/31 (`logs/phase6-manifest-check.log`) |
| DoD-02 | pass | backend check command BUILD SUCCESS (SpotBugs with `spotbugs-exclude.xml`, D-21); frontend `npm run check` exit 0; Semgrep results triaged below |
| DoD-03 | pass | coverage and mutation below, with scope, level and tool; thresholds are "record only" (`quality-requirements.md`) |
| DoD-04 | pass | `ArchitectureTest` (5 rules: S2 layers, framework-free domain, application uses only Spring transactions, independent adapters, no cycles in `si.konferenca.registration.(*)` and `.infrastructure.(*)`); AR-01/04/05/06/07 in the security table |
| DoD-05 | pass | no open Critical or High; F-06 lowered to Low with evidence and human approval (D-25, D-26) |
| DoD-06 | pass | `logs/phase6-runtime-demo.log`: stack healthy, registrations, storage, emails, export, restart |
| DoD-07 | pass | traceability table: 49/49 AC have tests and commits |
| DoD-08 | phase 7 | READMEs are written and checked from a clean clone in phase 7 |
| DoD-09 | phase 7 | release notes with the manual test list in phase 7 |
| DoD-10 | pass | input manifest 35/35 unchanged; every decision resolved or pending review (D-25 resolved by D-26) |
| DoD-11 | pass with finding | phase 3 commits touch no production source; freeze commit `213da7a` holds only the manifest, every listed file committed earlier; each story has a `feat` commit naming its US/AC; 6 commits exceed the size guide (F-10) |
| DoD-P01 | pass | demo: external and student registration → 201 |
| DoD-P02 | pass | demo: both rows and option rows in PostgreSQL; `<id>.json` copies, mode `rw-------`, owner `app` |
| DoD-P03 | pass | demo: Mailpit holds the participant confirmation and the organizer email (1 attachment) for each |
| DoD-P04 | pass | demo: export 200, workbook opens and contains the rows (Slovenian characters intact); without credentials 401; wrong password 401 |
| DoD-P05 | pass | `US001CaptchaLiveModeAcceptanceTest`: production verifier against a mocked siteverify endpoint; rejected token → 400, accepted token → 201; `CaptchaVerifierTest` for failure, malformed, 5xx, unreachable, read timeout |

## Measures

Scope and counting per `general/quality/measurement-scope.md`; exclusions from `docs/03_test-strategy.md` (entry points, type declarations).

### Test counts (final run)

| Component | unit | integration | acceptance | architecture | e2e |
|---|---|---|---|---|---|
| backend (JUnit) | 75 | 7 | 52 | 5 | — |
| frontend (Vitest / Playwright) | 21 | — | 13 | — | 4 |

### Coverage

| Component | Tool | Level | Line | Branch |
|---|---|---|---|---|
| backend | JaCoCo 0.8.12 | unit | 71.2 % (538/756) | 79.0 % (226/286) |
| backend | JaCoCo 0.8.12 | nonUnit (acceptance + integration) | 90.1 % (681/756) | 68.9 % (197/286) |
| backend | JaCoCo 0.8.12 | all | **95.2 %** (720/756) | **84.3 %** (241/286) |
| frontend | Vitest v8 | unit | 66.9 % (93/139) | 67.8 % (61/90) |
| frontend | Vitest v8 | nonUnit (component acceptance; e2e not instrumented) | 75.5 % (105/139) | 65.6 % (59/90) |
| frontend | Vitest v8 | all | **97.8 %** (136/139) | **90.0 %** (81/90) |

Unit-level backend figures were measured before F-01 added tests. Raw: `logs/coverage/`, `logs/phase6-coverage-*.log`.

### Mutation

| Component | Tool | Scope | Tests | score | scoreCovered | killed / total / covered |
|---|---|---|---|---|---|---|
| backend | PIT 1.30.0 | all production code minus entry point | unit + architecture (D-23) | 72.6 % | 96.2 % | 252 / 347 / 262 |
| frontend | Stryker 10.0.0, command runner (D-24) | all production code minus `main.tsx`, `vite-env.d.ts` | full Vitest suite (unit + acceptance) | 88.2 % (timed-out counted as killed; killed only: 40.7 %) | 88.2 % | 314 (145 killed + 169 timed out) / 356 / 356 |

Backend: the 85 mutants without coverage are in code reached only by the container-based tests excluded by D-23 (per class: SecurityConfiguration 15, JpaRegistrationStore 12, ApiExceptionHandler 11, WebConfiguration 9, RegistrationConfiguration 8, JsonCopyDirHealthIndicator 4, RateLimitFilter 4, persistence entities 6, controllers and DTOs 8, CoreConfiguration 2, CachedBodyRequest 2, others 4); the acceptance and integration tests cover these classes (nonUnit line coverage 90.1 %).

Surviving backend mutants after F-01, each classified (security, validation and persistence code individually):

| Class:line | Mutation | Classification |
|---|---|---|
| RegistrationValidator:85, :100, :114, :129 | return value replaced | equivalent: the value is discarded because the field error already makes `validate` throw |
| RateLimitFilter:58 (2) | cleanup condition | equivalent: only prunes expired windows; limits unchanged |
| RequestSizeFilter:37 | `>= 0` → `> 0` | equivalent: a zero-length body reads zero bytes on either path |
| FileRegistrationCopyWriter:66 | POSIX check negated | equivalent on Linux: `createTempFile` already creates `rw-------` files |
| Problems:16 | charset call removed | equivalent: problem bodies are ASCII |
| GoogleCaptchaVerifier:25 | connect timeout removed | not tested (needs an unroutable address, would be slow and flaky); read timeout is tested; accepted, Low |

Frontend (full run `logs/phase6-mutation-frontend-full.json`, 13 min 33 s): 42 survivors. Real unit gaps: the email pattern without its `$` anchor and the type-switch filtering of selected options (`RegistrationForm` lines 41, 51), fixed by F-12 and re-verified with a targeted run (`logs/phase6-mutation-frontend-f12.log`); the remaining lines 41 (optional chaining, ids always come from the option list) and 51 (uncheck then recheck yields the same selection) are equivalent. The other survivors are equivalent or presentation-only: `Accept` header literals and object shapes in `api.ts` (the fake backend ignores them), the 400/409 branch guards whose alternative paths also end in a failure message, effect dependency arrays and the `async`/`defer` flags of the reCAPTCHA script, initial empty-string state, and the `type="email"` literal (role and validation unchanged). Limitation: 169 mutants ended by timeout (React render loops and per-mutant Vitest start-up under 4 workers); between runs some flip between timeout and survived, so the figure varies by a few mutants. The client checks are not authoritative; the backend repeats every rule (SB-01).

### Code size and duplication

| Component | Production LOC | Test LOC | Duplication |
|---|---|---|---|
| backend | 1,900 (52 files) | 3,023 (33 files) | 0 (PMD CPD 3.26.0) |
| frontend | 581 (9 files) | 968 (14 files) | 0 % (jscpd 5.4.0) |

`cloc` from `aldanial/cloc:2.10` (reports 1.98, D-03); generated code and lock files excluded (`logs/phase6-cloc.log`).

## Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| all | local, `scripts/compose.sh up -d --build` (D-22), plain HTTP on 127.0.0.1 | health of all four containers (NFR-04) | all `healthy` |
| backend + frontend nginx | local | external and student registration through `http://127.0.0.1:8080/api` (DoD-P01) | 201, 201 |
| backend + PostgreSQL + volume | local | rows, option rows, JSON copies (DoD-P02, BR-07) | present; copies `rw-------` owned by `app` |
| backend + Mailpit | local | participant and organizer emails (DoD-P03) | 4 messages, organizer ones with 1 attachment |
| backend | local | export with, without and with wrong credentials (DoD-P04) | 200 (workbook opens, rows with č, š, ž), 401, 401 |
| all | local | `down` then `up` (NFR-02) | both rows and both copies present, export 200, all healthy |
| frontend | local | full UI flows by Playwright (e2e 4/4) incl. NFR-01 | pass |
| backend | local | log review for personal data and secrets (ES-07) | 0 matches of names, emails or "password" |

## Security controls

| ID | Implemented in | Checked by |
|---|---|---|
| SB-01 | `RegistrationValidator` (server side) | `RegistrationValidatorTest`, AC-001-04..12, AC-002-04..07 |
| SB-02 | `SecurityConfiguration` (only `/api/organizer/**` needs `ORGANIZER`) | AC-008-03/04, integration test |
| SB-03 | BCrypt hash at startup; secrets only from environment; no defaults | inspection of `SecurityConfiguration`, `application.properties`; `StartupGuardsTest` |
| SB-04 | TLS at the reverse proxy (production); STARTTLS via `MAIL_TLS`; Google over HTTPS; local bound to 127.0.0.1 | inspection, `docker-compose.yml` ports; manual test in release notes |
| SB-05 | JPA bound parameters, Jackson, React escaping, plain-text email, string cells | AC-006-03, AC-008-05, Semgrep |
| SB-06 | `RateLimitFilter` | `FiltersTest`, integration test (429 per client) |
| SB-07 | `ApiExceptionHandler`, `ProblemAuthenticationEntryPoint`, `server.error.*` | integration test (malformed, 404), AC-007-03, AC-008-06, runtime log review |
| SB-08 | Dependency-Check 12.1.0 (NVD), `npm audit` | F-07, F-08; no Critical or High in shipped code |
| SB-09 | Semgrep 1.177.0, gitleaks v8.30.1, SpotBugs, PMD | findings below |
| SB-10 | Spring Security headers; nginx headers | integration test `apiResponsesCarryTheSecurityHeaders`; `curl -I` on the frontend |
| SB-11 | non-root `app` (backend) and `nginx` users; `cap_drop: ALL`, `no-new-privileges`; ports on 127.0.0.1 | Dockerfiles, compose, runtime file owner `app` |
| SB-12, SB-13, SB-14 | only BR-01 fields + options + consent; retention D-15; consent unchecked, stored with wording and time | AC-001-02, AC-002-02, AC-005-01; release notes (retention task) |
| SR-01 | `RegistrationService` + `GoogleCaptchaVerifier` | AC-001-13, AC-002-08, DoD-P05 |
| SR-02 | `StartupGuards` | `StartupGuardsTest` |
| SR-03 | `RateLimitFilter`, `RequestSizeFilter`, nginx `client_max_body_size` | `FiltersTest`, integration test (413, 429) |
| SR-04 | `RegistrationValidator.options` | AC-001-08/09/10, AC-002-06 |
| SR-05 | `MailNotifier` plain text, fixed subjects, validated address | AC-006-03, `MailNotifierTest` |
| SR-06 | `OrganizerHttpsFilter` | `FiltersTest`, integration test |
| SR-07 | `MailNotifier.organizerText`, `PoiWorkbookWriter` fixed columns | AC-007-03, AC-008-06 |
| AR-01 | frontend uses relative `/api` paths only | inspection of `api.ts`; e2e through nginx |
| AR-04 | option file, restart | AC-003-02, AC-003-03 |
| AR-05 | `RegistrationService` transaction + copy cleanup | AC-005-03/04, `RegistrationServiceTest` |
| AR-06 | Flyway `V1__registration.sql`, `ddl-auto=validate` | acceptance tests start against an empty database |
| AR-07 | site key served by `/api/settings`; no secret in the bundle | integration test `settingsExposeNoSecret` |

## Traceability

AC → tests (file names) → commits (`feat` and `test` commits naming the story or AC).

| AC | Tests | Commits |
|---|---|---|
| AC-001-01 | us001-external-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-02 | us001-external-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-03 | US001CaptchaLiveModeAcceptanceTest.java, US001ExternalRegistrationAcceptanceTest.java, us001-external-registration.e2e.spec.ts | de033ab 3aa465d ab82af5 bd92804 3d6a35c 8aa888c |
| AC-001-04 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-05 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-06 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-07 | US001ExternalRegistrationAcceptanceTest.java, us008-export-and-slovenian-characters.e2e.spec.ts | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-08 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-09 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-10 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-11 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-12 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-13 | US001CaptchaLiveModeAcceptanceTest.java, US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 bd92804 3d6a35c 8aa888c |
| AC-001-14 | US001ExternalRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-15 | us001-external-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-001-16 | us001-external-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 3d6a35c 8aa888c |
| AC-002-01 | us002-student-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 667af2a |
| AC-002-02 | us002-student-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 667af2a |
| AC-002-03 | US002StudentRegistrationAcceptanceTest.java, us002-student-registration.e2e.spec.ts | de033ab 3aa465d ab82af5 667af2a |
| AC-002-04 | US002StudentRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 667af2a |
| AC-002-05 | US002StudentRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 667af2a |
| AC-002-06 | US002StudentRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 667af2a |
| AC-002-07 | US002StudentRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 667af2a |
| AC-002-08 | US002StudentRegistrationAcceptanceTest.java | de033ab 3aa465d ab82af5 667af2a |
| AC-002-09 | us002-student-form.acceptance.test.tsx | de033ab 3aa465d ab82af5 667af2a |
| AC-003-01 | US003OptionsAcceptanceTest.java | b6fb493 c944cef |
| AC-003-02 | US003OptionConfigurationAcceptanceTest.java | b6fb493 c944cef |
| AC-003-03 | US003OptionConfigurationAcceptanceTest.java | b6fb493 c944cef |
| AC-004-01 | us004-confirmation.acceptance.test.tsx | de033ab c2663aa |
| AC-004-02 | us004-confirmation.acceptance.test.tsx | de033ab c2663aa |
| AC-004-03 | us004-confirmation.acceptance.test.tsx | de033ab c2663aa |
| AC-005-01 | US005StorageAcceptanceTest.java | 98eac54 aedad14 |
| AC-005-02 | US005StorageAcceptanceTest.java | 98eac54 aedad14 |
| AC-005-03 | US005StorageAcceptanceTest.java | 98eac54 aedad14 |
| AC-005-04 | US005DatabaseUnavailableAcceptanceTest.java | 98eac54 aedad14 |
| AC-006-01 | US006ParticipantEmailAcceptanceTest.java | aac73dd 709de91 |
| AC-006-02 | US006ParticipantEmailAcceptanceTest.java, us008-export-and-slovenian-characters.e2e.spec.ts | aac73dd 709de91 |
| AC-006-03 | US006ParticipantEmailAcceptanceTest.java | aac73dd 709de91 |
| AC-006-04 | US006MailServerUnavailableAcceptanceTest.java | aac73dd 709de91 |
| AC-006-05 | US006ParticipantEmailAcceptanceTest.java | aac73dd 709de91 |
| AC-007-01 | US007OrganizerNotificationAcceptanceTest.java | aac73dd 3080745 |
| AC-007-02 | US007OrganizerNotificationAcceptanceTest.java, us008-export-and-slovenian-characters.e2e.spec.ts | aac73dd 3080745 |
| AC-007-03 | US007OrganizerNotificationAcceptanceTest.java | aac73dd 3080745 |
| AC-008-01 | US008ExportAcceptanceTest.java | 4d22a85 6031e5b 8aa888c |
| AC-008-02 | US008ExportAcceptanceTest.java, us008-export-and-slovenian-characters.e2e.spec.ts | 4d22a85 6031e5b 8aa888c |
| AC-008-03 | US008ExportAcceptanceTest.java, us008-export-and-slovenian-characters.e2e.spec.ts | 4d22a85 6031e5b 8aa888c |
| AC-008-04 | US008ExportAcceptanceTest.java | 4d22a85 6031e5b 8aa888c |
| AC-008-05 | US008ExportAcceptanceTest.java | 4d22a85 6031e5b 8aa888c |
| AC-008-06 | US008ExportAcceptanceTest.java | 4d22a85 6031e5b 8aa888c |

## Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-01 | Low | PIT 1.30.0 | 14 surviving mutants that unit tests should have caught (validation boundary, option loader boundaries and types, request-size boundaries, organizer mail lines, captcha read timeout, list copies) | fixed in `2c206b7`; re-run: 10 survivors, all classified above |
| F-02 | Low | gitleaks v8.30.1 | 19 `generic-api-key` hits: Spring Security's per-start generated password printed by the phase 3 bootstrap skeleton into committed logs; random, never used, not a `.env` value | redacted in the working tree (`1326f5c`), re-scan 0; history kept (no rewriting) |
| F-03 | Medium | runtime demonstration (agent) | a curl password prompt echoed part of the `ORGANIZER_USERNAME` value into the session output and an uncommitted log | line removed before commit, leak checks clean; the session transcript keeps the fragment: the human should change `ORGANIZER_USERNAME` in `.env`; check rewritten to build the header in the shell |
| F-04 | Medium | Semgrep (`request-host-used`, WARNING) | nginx forwarded the client `Host` header to the backend | fixed in `cfbc555` (fixed `Host backend`); re-scan clean, e2e 4/4 |
| F-05 | Medium | Semgrep (`npm-missing-minimum-release-age`) | `.npmrc` has no `min-release-age` | accepted: the setting needs npm ≥ 11.10, the pinned npm is 11.6.2; exact pins, committed lock and `npm ci` mitigate; open item for a future npm upgrade |
| F-06 | High as reported → Low | Semgrep (`use-of-basic-authentication`, ERROR) | organizer export uses HTTP Basic | lowered to Low and accepted with the evidence in D-25; human approval D-26 |
| F-07 | Low (reported Medium, 5.3) | Dependency-Check 12.1.0 | CVE-2025-15104 matched to `hibernate-validator` 9.1.3.Final | false positive: the CVE concerns the Nu Html Checker (CPE `validator:validator`); the code uses no Bean Validation; accepted |
| F-08 | Medium | `npm audit` | `qs` / `typed-rest-client` (moderate) under `@stryker-mutator/core` 10.0.0 | accepted: dev-only mutation tool, not in the shipped bundle (`npm audit --omit=dev`: 0) |
| F-09 | Low | Semgrep | partial parse of the third-party `backend/mvnw` script | informational |
| F-10 | Low | git history | 6 commits exceed the size guide without a stated reason except `8aa888c` (see `logs/phase6-commit-size.log`) | accepted; recorded as a divergence; history is not rewritten |
| F-12 | Low | Stryker 10.0.0 | 2 frontend unit gaps: email pattern anchor, type-switch filtering of selections | fixed (tests in `validation.test.ts`, `RegistrationForm.test.tsx`); targeted Stryker re-run kills them |
| F-11 | Low | Stryker 10.0.0 | the `vitest-runner` does not work with Vitest 5.0.3 (invalid 16.9 % score) | measured with the command runner instead (D-24) |

Suppressed earlier: CVE-2025-7962 on `angus-activation` (false positive, D-05/D-09); still suppressed and still the only suppression.

Open after triage: Critical 0, High 0, Medium 3 accepted with reasons (F-03 pending the human's `.env` change, F-05, F-08).

## Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-01 | 7 unit tests added or extended (`2c206b7`) | PIT re-run: survivors 22 → 10, score 69.2 % → 72.6 % |
| 2 | F-02 | redacted generated passwords in 8 phase 3 logs (`1326f5c`) | gitleaks on the tree: 0 |
| 3 | F-04 | nginx `proxy_set_header Host backend` (`cfbc555`) | Semgrep re-run without the finding; frontend image rebuilt; e2e 4/4 |
| 5 | F-12 | 2 frontend unit tests | targeted Stryker re-run on `validation.ts`, `RegistrationForm.tsx` |
| 4 | F-03 | removed the leaked log line; safe header-based check | `.env` leak checks: no file, `0` commit messages |
