# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Written from | Frozen |
|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/`, `backend/src/test/resources/acceptance/` | JUnit 6 via `./mvnw test`; MockMvc through the HTTP API; PostgreSQL and Mailpit via Testcontainers; reCAPTCHA test mode, and a local stub `siteverify` endpoint for the production verifier (DoD-P05) | `acceptance-criteria.md`, `02_contracts/openapi.json`, `database.sql`, `registration-copy.schema.json`, `emails.json`, `conference-config.schema.json`, `recaptcha.json` | yes |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library (jsdom), `fetch` stubbed per contract | `acceptance-criteria.md`, `02_contracts/ui-registration-form.json`, `openapi.json` | yes |
| End-to-end | frontend + backend | `frontend/e2e/` (one file per scenario E2E-01 … E2E-05) | Playwright (Chromium) against `docker compose` in `02_output/`; checks PostgreSQL (`psql` in the container), JSON copies (`cat` in the backend container), Mailpit API and the export | `e2e-scenarios.md` | yes |
| Architecture | backend | `backend/src/test/java/si/konferenca/registration/architecture/` | ArchUnit, `./mvnw test` | specification section 2 | no (written in phase 4 with the packages) |
| Unit / integration | both | any other test path | JUnit / Vitest | implementation | no (phase 5) |

## Measurement exclusions

Decided in phase 3 before any test was written (`general/quality/measurement-scope.md`, `project/02_design/quality-requirements.md` "Mutation scope").

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | `@SpringBootApplication` entry point, no logic |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/**/*.d.ts` | type declarations (none planned) |

Test code (`backend/src/test/**`, `frontend/tests/**`, `frontend/e2e/**`, `*.test.ts(x)`) never counts. No generated code is planned.

## Phase 3 run (frozen tests against the bootstrap skeleton)

Run on 2026-10-12 against the phase 0 skeletons (logs: `out/logs/03_backend-acceptance-run.log`, `03_frontend-acceptance-run.log`, `03_e2e-run.log`). No test passes on bootstrap code; none fails with a build, configuration or harness error.

| Component / level | Tests | Passed | Failed | Reason (behaviour missing) |
|---|---|---|---|---|
| backend acceptance | 92 | 0 | 92 | API not implemented: the skeleton's default security answers 401/403 instead of 200/201/400; the options configuration is not read, so invalid files do not stop startup (AC-003-03); no error body (AC-005-03) |
| frontend acceptance | 10 | 0 | 10 | the skeleton page has no registration type choice, form, confirmation or error display |
| end-to-end | 5 | 0 | 5 | the served skeleton page has no form (E2E-01, E2E-03, E2E-04); the skeleton backend does not start without its configuration wiring, so `/api` answers 502 (E2E-02, E2E-05) |

Per story: US-001 29 backend + 3 production-verifier + 1 frontend + E2E-01; US-002 29 backend + 2 production-verifier + 2 frontend + E2E-02; US-003 6; US-004 7 frontend + E2E-03; US-005 6 + E2E-04; US-006 5; US-007 3; US-008 9 + E2E-05.

Harness defects found and fixed before the freeze: PostgreSQL connection exhaustion by cached test contexts (pool size 4, `max_connections=300`); AC-005-03 passed on the skeleton for the wrong reason (any 403), so it also requires the contract's error body; E2E-02/E2E-05 assert the HTTP status before reading the body.

Self-check before the freeze (write-acceptance-tests step 5): Mailpit API shapes verified against the pinned image (full message has `To`, `Cc`, `Bcc` arrays, `HTML`, `Text`, `Attachments[].PartID`; `/headers` map; `/part` returns raw bytes); timestamps read as `OffsetDateTime` (not `Timestamp`) and compared with 1 ms tolerance for the microsecond precision of `timestamptz`; byte arrays compared whole with `isEqualTo`; AC-003-03 first starts the same settings with a valid file so only the configuration differs; AC-008-04 has positive controls (HTTPS from remote and plain HTTP from localhost are served); no assertion contradicts another in the same test.

## First complete run (before any fix)

Phase 5 start, final phase 4 code, no unit tests yet (logs: `out/logs/05_backend-first-run.log`, `05_frontend-first-run.log`, `04_e2e-run.log`).

| Component | Level | Passed | Failed |
|---|---|---|---|
| backend | acceptance | 92 | 0 |
| backend | architecture | 6 | 0 |
| frontend | acceptance | 10 | 0 |
| frontend + backend | end-to-end | 5 | 0 |
| total | | 113 | 0 |

No failure to classify. Defects found earlier during phase 4 (before this run) and fixed in code: JSON attachment line endings on Linux (AC-007-02, commit "fix: keep JSON attachment byte-identical…").

Coverage at this run (acceptance + architecture tests only): backend line 89.83 % (707/787), branch 72.80 % (JaCoCo 0.8.12); frontend line 84.07 % (132/157), branch 70.64 % (Vitest v8).

## Final run (phase 6)

Same code as phase 5 (no change in phase 6): backend 241 passed / 0 failed (138 unit, 5 integration, 92 acceptance, 6 architecture), frontend 52 / 0 (42 unit, 10 acceptance), end-to-end 5 / 0 against the rebuilt stack (`out/logs/06_runtime-demo.log`, `06_final-checks.log`). Coverage and mutation scores were not re-measured because no code or test changed after phase 5.

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 92 / 0 | — | — | — | JUnit 6, MockMvc, Testcontainers |
| backend | integration | 5 / 0 | — | — | — | JUnit 6, MockMvc, Testcontainers |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 138 / 0 | 86.91 % (684/787) | 96.80 % (242/250) | — | JaCoCo 0.8.12 |
| backend | non-unit | 103 / 0 | 90.98 % (716/787) | 74.40 % (186/250) | — | JaCoCo 0.8.12 |
| backend | all levels | 241 / 0 | 99.24 % (781/787) | 98.80 % (247/250) | raw 91.44 %, adjusted 91.44 % (267 killed of 292; 24 survived, 1 no coverage; 0 equivalent); scope `si.konferenca.registration.*` minus `RegistrationApplication`; tests: all 241 JUnit tests (unit, integration, acceptance, architecture) | PIT 1.30.0 + junit5 plugin 1.2.3, default mutators |
| frontend | acceptance | 10 / 0 | 84.07 % (132/157) | 70.64 % (77/109) | — | Vitest 4.1.11, v8 |
| frontend | unit | 42 / 0 | 98.72 % (155/157) | 92.66 % (101/109) | — | Vitest 4.1.11, v8 |
| frontend | non-unit | 10 / 0 | 84.07 % (132/157) | 70.64 % (77/109) | — | Vitest 4.1.11, v8 |
| frontend | all levels | 52 / 0 | 100 % (157/157) | 99.08 % (108/109) | raw 90.24 % (370 killed of 410 valid mutants; 39 survived, 1 no coverage; 1 compile-error mutant not counted by the tool, 90.02 % if counted), adjusted 90.24 %, 0 equivalent; scope `src/**/*.{ts,tsx}` minus tests, `*.d.ts`, `main.tsx`; tests: all 52 Vitest tests | Stryker 10.0.0 + vitest-runner |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 against docker compose |

Code size (cloc 2.10 image, `out/logs/05_cloc.log`): backend production 1 770 lines (Java 1 675), test 3 543; frontend production 622 (TypeScript 572), test 1 369. Duplication: backend CPD (PMD 3.26.0, 100 tokens) no duplicates; frontend jscpd 5.4.0 0.00 %.

Quality gate (`project/02_design/quality-requirements.md`: line ≥ 95 %, adjusted mutation ≥ 90 %): met by both components at the first full mutation run; 0 survivor rounds; no further tests added. Tests at the first mutation run: backend 241, frontend 52. The mutation runs exclude the 5 e2e tests (D-16). Logs: `05_pit-full.log`, `05_pit-mutations.xml`, `05_stryker-full.log`, `05_stryker-mutation.json`.

Notable survivors (not equivalent, left because the gate is met): `SecurityConfiguration` builder code runs once when the Spring context starts, so PIT's in-process mutants of it are never re-executed by later tests; `RateLimitFilter` window-cleanup boundaries; `MailNotifier` attachment disposition; frontend `App` loading/unmount guards and `RegistrationForm` CSS class names.
