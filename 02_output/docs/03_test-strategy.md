# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Frozen |
|---|---|---|---|---|
| acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 via Maven Surefire, Testcontainers PostgreSQL and Mailpit | yes |
| acceptance | frontend | `frontend/tests/acceptance/` | Vitest 4 + Testing Library (jsdom) | yes |
| e2e | all | `frontend/e2e/` | Playwright 1.63 Chromium against the local compose stack | yes |
| unit, integration, architecture | each | other test paths | phase 5 | no |

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`@SpringBootApplication` with `main` only); excluded by `quality-requirements.md` |
| `frontend/src/main.tsx` | bootstrap file that only mounts `App`; excluded by `quality-requirements.md` |
| `frontend/src/**/*.d.ts` | type declarations, no runtime code |
| `frontend/src/**/*.test.{ts,tsx}`, `frontend/tests/**`, `frontend/e2e/**`, `backend/src/test/**` | test code |

No generated code exists in either component. Nothing else is excluded.

## Phase 3 run (frozen tests against the bootstrap skeleton)

Run on 2026-10-13 before any production code beyond the phase 0 skeleton (logs: `logs/03_backend-acceptance-first-run.log`, `logs/03_frontend-acceptance-first-run.log`, `logs/03_e2e-first-run.log`).

| Suite | Tests | Passed | Failed | Reason for the failures |
|---|---|---|---|---|
| backend acceptance (JUnit, 13 classes) | 80 | 2 | 78 | 75: the skeleton has no API, Spring Security's default answers 401 where 201/400/200 are expected; 3 (AC-003-03): the skeleton starts with an invalid options file |
| frontend acceptance (Vitest, 3 files) | 7 | 0 | 7 | the page has no type choice (`radio "External participant"` not found) |
| e2e (Playwright, 5 files) | 5 | 0 | 5 | E2E-01…04: no type choice on the page; E2E-05: no backend serves `/api` yet (the stack ran PostgreSQL, Mailpit and the frontend; the skeleton backend cannot start because mapping the spec §3 environment variables is production configuration, written in phase 4) |

No failure comes from a build, compile or harness error (backend: 0 errors, only assertion failures).

Tests that pass on the bootstrap code:

| Test | Why it passes |
|---|---|
| `InvalidConfigurationAcceptanceTest` "AC-003-03 control: the same start-up with a valid configuration succeeds" | it checks that the start-up harness itself works; the skeleton already starts |
| `JsonCopyFailureAcceptanceTest` (AC-005-03), first version | passed for the wrong reason (skeleton 401 satisfied "not 201"); tightened before the freeze to require a 5xx server error (spec §4 step 5), now fails as expected (commit 075e7e9) |

### Self-check before the freeze (skill step 5)

- Timestamps from PostgreSQL are read as `java.sql.Timestamp` and converted explicitly (`StorageAcceptanceTest.instant`).
- Workbook and attachment checks compare text values or parsed JSON trees, not byte sequences; the e2e workbook reader unzips the file and decodes XML entities.
- Mailpit search does not match non-ASCII terms (verified against `axllent/mailpit:v1.31.1`); E2E-04 searches by the ASCII run suffix.
- Rejection checks first assert the status, then "nothing stored" (rows, copies, emails), so they cannot pass on an empty database alone.
- AC-008-04 contains a control request from localhost that must succeed, so the refusal cannot come from the credentials.
- AC-003-03 contains a control start-up with a valid file, so the failure cannot come from the harness.
- Cross-test email leakage is avoided because the specification sends emails within the request, after commit (spec §4 step 7); every test resets database, copies and Mailpit first.
- Frontend tests address elements only by role, label and the `data-testid` values of `ui-contract.md`.
- All tests are formatted (Spotless, Prettier) and lint-clean (ESLint) before the freeze.

## First complete run (before any fix)

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
