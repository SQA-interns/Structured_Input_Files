# Test strategy (as executed)

> Written in: phases 3, 5 and 6 · Source: `general/quality/test-strategy.md`, `general/quality/measurement-scope.md`, `general/phases.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Interface |
|---|---|---|---|---|
| acceptance (frozen) | backend | `backend/src/test/java/si/konferenca/registration/acceptance/`, fixtures in `backend/src/test/resources/acceptance/` | JUnit 6 via `./mvnw test` | HTTP API through MockMvc and the full Spring Security filter chain; storage tables (`database.sql`) over JDBC; JSON copy directory; Mailpit HTTP API; PostgreSQL and Mailpit from Testcontainers |
| acceptance (frozen) | frontend | `frontend/src/acceptance/` | Vitest via `npm test` | rendered UI (`ui.md`), `fetch` replaced by a test double of `openapi.json` |
| end-to-end (frozen) | all | `frontend/e2e/` | Playwright via `npm run e2e` | browser against `docker compose` stack, Mailpit API, `docker compose exec` for PostgreSQL and the JSON copy |
| unit / integration (test-first) | both | any other test path | same runners | written in phase 4 |

### Acceptance criteria → frozen tests

| AC | Test |
|---|---|
| AC-001-01..07 | `ExternalRegistrationAcceptanceTest` (30 cases) |
| AC-001-08 | `externalForm.acceptance.test.tsx` |
| AC-002-01..06 | `StudentRegistrationAcceptanceTest` (28 cases) |
| AC-002-07 | `studentForm.acceptance.test.tsx` |
| AC-003-01 | `FormConfigAcceptanceTest` |
| AC-003-02 | `ChangedConfigurationAcceptanceTest` |
| AC-003-03 | `InvalidConfigurationAcceptanceTest` (3 cases) |
| AC-004-01..04 | `confirmation.acceptance.test.tsx` (5 cases) |
| AC-005-01..04 | `StorageAcceptanceTest` (5 cases) |
| AC-006-01..02 | `ParticipantEmailAcceptanceTest` |
| AC-006-03 | `MailServerUnavailableAcceptanceTest` |
| AC-007-01..02 | `OrganizerNotificationAcceptanceTest` |
| AC-008-01..04 | `ExportAcceptanceTest` |
| E2E-01, E2E-02 | `e01-e02-registration.e2e.ts` (one test each) |
| E2E-03 | `e03-field-errors.e2e.ts` |
| E2E-04 | `e04-slovenian-characters.e2e.ts` |
| E2E-05 | `e05-export.e2e.ts` |

All 35 criteria have at least one test; 5 end-to-end tests, one per scenario, no other.

### Phase 3 run against the bootstrap skeleton (before the freeze)

| Suite | Passed | Failed | Reason (one line per group) | Log |
|---|---|---|---|---|
| backend acceptance | 0 | 77 | assertion: the skeleton answers 403/401 instead of 201/400/200 (no endpoints, default security); AC-003-03: the backend starts with invalid configuration | `out/logs/phase3-backend-acceptance-full-run.log` |
| frontend acceptance | 0 | 7 | Testing Library cannot find the "Registration type" radios (the skeleton renders an empty page) | `out/logs/phase3-frontend-acceptance-run.log` |
| end-to-end | 0 | 5 | the page has no form (radio not found within 10 s); E2E-05: export answers 401 instead of 200 | `out/logs/phase3-e2e-run.log` |

No test passes on bootstrap code; no test fails on a build, configuration or harness error.

### Self-check before the freeze

- Timestamps are read with `getObject(..., OffsetDateTime.class)`; byte comparisons use `isEqualTo` on whole arrays (attachment versus JSON copy); a response without content type is compared as text, not `null`.
- Helpers tolerate the missing schema of the skeleton (`to_regclass`), so the first failure is the behavioural assertion.
- Emails are matched by a unique participant address or the registration id, so asynchronous mail from another test cannot satisfy or break an assertion; "no email" waits one second.
- The export column order and the option order follow `excel-export.md` (configuration order), which the storage contract can reproduce.
- The e2e workbook reader was checked against a workbook written by POI 5.5.1 (shared strings, blank cells, escaped text, Slovenian characters).
- The scenario files run in scenario order (`e01` … `e05`), so E2E-05 exports the registrations of E2E-01..04.

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point, no logic (`quality-requirements.md`, mutation scope) |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app |
| `frontend/src/**/*.d.ts` | type declarations, no runtime code |
| `frontend/src/test-setup.ts` | test configuration, not production code |

Nothing else is excluded; generated code (`target/`, `dist/`) is never measured.

## TDD baseline (phase 5, before any added test)

<!-- Coverage reached by the tests written test-first in phase 4, measured before any test-after test is added. Same figures in `run-log.json` → `codeMetrics.tddBaseline`. -->

| Component | Tests (unit / integration) | Line coverage (all) | Branch coverage (all) | Tools |
|---|---|---|---|---|
| backend | 232 (131 unit / 18 integration, plus 77 acceptance and 6 architecture) | 96.34 % (816/847); unit only 81.11 %, non-unit only 88.31 % | 87.02 % (228/262); unit 83.21 %, non-unit 68.70 % | JaCoCo 0.8.12, `mvnw test jacoco:report`, fresh `jacoco.exec` per run |
| frontend | 56 (49 unit, plus 7 acceptance) | 97.85 % (137/140); unit only 97.85 %, non-unit only 79.28 % | 93.58 % (73/78); unit 93.58 %, non-unit 67.94 % | Vitest 4.1.11 with @vitest/coverage-v8 |

Measured on commit `4059c0d` before any test-after test. Both components are above the line threshold (95 %) with the test-first tests alone, so no coverage tests are added. Scope: production code as in "Measurement exclusions"; test helpers that live under `src/` (`frontend/src/testConfig.ts`, `frontend/src/acceptance/**`) are test code and never count. Logs: `out/logs/phase5-baseline-*`.

## First complete run (before any fix)

Start of phase 5, after the TDD build, on commit `78133e6` (same production code as the baseline).

| Level | Component | Passed | Failed |
|---|---|---|---|
| unit | backend / frontend | 131 / 49 | 0 |
| integration | backend | 18 | 0 |
| architecture | backend | 6 | 0 |
| acceptance (frozen) | backend / frontend | 77 / 7 | 0 |
| end-to-end (frozen) | stack | 5 | 0 |
| **total** | | **293** | **0** |

No failure, so nothing to classify and no fix. Logs: `out/logs/phase4-gate-backend.log`, `out/logs/phase4-gate-frontend-test.log`, `out/logs/phase4-e2e-run-1.log`.

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
