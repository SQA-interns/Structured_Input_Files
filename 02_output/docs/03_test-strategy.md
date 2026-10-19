# Test strategy (as executed)

> Written in: phases 3, 5 and 6 · Source: `general/quality/test-strategy.md`, `general/quality/measurement-scope.md`, `general/phases.md` · Agent: writes

## Test levels as executed

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

## First complete run (before any fix)

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
