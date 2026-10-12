# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Interface used |
|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 via `./mvnw test` | HTTP against the started app (random port), PostgreSQL (Testcontainers), Mailpit API (Testcontainers), JSON copy directory |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library (`npm test`) | rendered UI, `fetch` replaced by a stub that speaks the OpenAPI contract |
| End-to-end | both | `frontend/e2e/` | Playwright (`npm run e2e`) | browser against `docker compose up` in `02_output/`, PostgreSQL via `docker compose exec`, Mailpit API, export over HTTP |
| Unit, integration, architecture | both | other test paths | as above | written in phase 5 |

### Phase 3 run (frozen tests against the bootstrap skeleton, before any production code)

| Suite | Tests | Passed | Failed | Reason (one line per group) |
|---|---|---|---|---|
| backend acceptance (`./mvnw test`, log `out/logs/03_backend-acceptance-first-run.log`) | 84 | 0 | 84 | every request answers 401: the skeleton has only Spring Security defaults, no API (registration, form config, export); AC-003-03 fails on its control start (no form config served). One test (consents of AC-003-01) raised NoSuchElementException on the 401 body; it now asserts the status first (commit 19f93f0), so it fails on the assertion. |
| frontend acceptance (`npm test`) | 10 | 0 | 10 | the skeleton renders an empty page: no "Registration type" radios, so every test fails at "Unable to find role radio" |
| end-to-end (`npm run e2e`, log `out/logs/03_e2e-first-run.log`) | 5 | 0 | 5 | the stack starts (PostgreSQL, Mailpit, frontend healthy) but the skeleton backend provides no API and no database configuration, so `GET /api/form-config` through the frontend proxy answers 502 |

No test passes on bootstrap code. Harness self-checks before the freeze: Testcontainers PostgreSQL and Mailpit start and the HTTP client reaches the app (401 answers); e2e helpers checked against the running stack (`psql` returns UTF-8 text unchanged, Mailpit search answers) and the xlsx reader parsed a POI-written workbook (2 rows, shared strings with Č, Š, Ž, & and < decoded).

Assertion self-check (write-acceptance-tests step 5): timestamps are read with `getObject(..., OffsetDateTime.class)`; byte comparisons use whole-array equality; every rejection test checks status, field key, and absence of row, copy and email for a unique address; status and field-error contracts come from `02_contracts/openapi.json`; UI queries use only the accessible names of `02_contracts/ui.md`; e2e tests read option names and consents from `GET /api/form-config`, so they do not depend on the shipped configuration.

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
