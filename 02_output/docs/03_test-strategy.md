# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Interface used |
|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 via `./mvnw test` | HTTP against the started app (random port), PostgreSQL (Testcontainers), Mailpit API (Testcontainers), JSON copy directory |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library (`npm test`) | rendered UI, `fetch` replaced by a stub that speaks the OpenAPI contract |
| End-to-end | both | `frontend/e2e/` | Playwright (`npm run e2e`) | browser against `docker compose up` in `02_output/`, PostgreSQL via `docker compose exec`, Mailpit API, export over HTTP |
| Unit, integration, architecture | both | other test paths | as above | written in phase 5 |

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`@SpringBootApplication`, no logic); listed as the only backend exclusion in `quality-requirements.md` |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app; listed in `quality-requirements.md` |
| `frontend/src/**/*.d.ts` | type declarations, no runtime code |

Nothing else is excluded. Test code (`backend/src/test/**`, `frontend/tests/**`, `frontend/e2e/**`) never counts.

## First complete run (before any fix)

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
