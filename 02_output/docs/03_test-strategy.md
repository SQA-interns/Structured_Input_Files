# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location (frozen if marked) | Tool | Runs against | Command |
|---|---|---|---|---|---|
| acceptance (frozen) | backend | `backend/src/test/java/si/konferenca/registration/acceptance/`, `backend/src/test/resources/acceptance/` | JUnit 6, AssertJ, Testcontainers 2 (`postgres:16.15-alpine`, `axllent/mailpit:v1.31.1`), JDK `HttpClient`, JSON Path, Apache POI | the application on a random port, configured only through the S11 variables; observed through the API, the `database.md` tables (JDBC), the JSON copy directory, Mailpit's API, and a JDK `HttpServer` mock of Google siteverify | `./mvnw -B test` |
| acceptance (frozen) | frontend | `frontend/src/acceptance/` | Vitest 5, Testing Library, jsdom | `App` with `fetch` replaced by a fake backend serving the `openapi.yaml` responses | `npm test` |
| e2e (frozen) | both | `frontend/e2e/`, `frontend/playwright.config.ts` | Playwright 1.63 (Chromium) | the compose stack at `E2E_BASE_URL` (default `http://127.0.0.1:8080`) with Mailpit at `E2E_MAILPIT_URL` (default `http://127.0.0.1:8025`); organizer credentials from `E2E_ORGANIZER_USERNAME`/`E2E_ORGANIZER_PASSWORD`; relies on the local default option file being `02_contracts/examples/conference-options.example.json` | `npx playwright test` |
| architecture | backend | phase 5 | ArchUnit | compiled classes | `./mvnw -B test` |
| unit, integration | both | phase 5 | JUnit / Vitest | | |

Harness notes:
- The backend tests also set `spring.datasource.*` (and, in AC-005-04, a 3 s Hikari connection timeout) so the bootstrap skeleton starts before the configuration exists; the application under test must read the S11 variables.
- AC-005-03 makes the JSON copy directory read-only, so the backend tests must not run as root.
- AC-005-04 stops a database of its own; AC-006-04 points SMTP to a closed port; AC-001-13 (live path, DoD-P05) uses the siteverify mock.

AC → test: every `AC-nnn-nn` id appears in at least one test name (`@DisplayName`, parameterized name or method `acNNN_NN_…`, Vitest/Playwright title); checked by script on 2026-10-12: 49 of 49.

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`measurement-scope.md` default) |
| `frontend/src/main.tsx` | application entry point (mounts `App`) |
| `frontend/src/vite-env.d.ts` | type declarations only, no executable code |

Mutation scope: `quality-requirements.md` lists no paths, so the default applies: all production code of each component minus the rows above. Backend: PIT over `si.konferenca.registration.*`; frontend: Stryker over `src/**/*.ts(x)` without tests.

## Phase 3 run (before any production code)

Recorded 2026-10-12 against the bootstrap skeleton (logs in `out/logs/phase3-*`). Nothing passes on bootstrap code, so no test is listed as passing only because of it.

| Suite | Tests | Passed | Failed | Errors | Reason |
|---|---|---|---|---|---|
| backend acceptance | 52 | 0 | 52 | 0 | every API call answers 401 (Spring Security default of the skeleton) where 201/400/409/500/200 is expected; option file and startup checks absent (AC-003-02/03) |
| frontend acceptance | 13 | 0 | 13 | 0 | the skeleton `App` renders no form: no `Register` button |
| e2e | 4 | 0 | 4 | 0 | run against the skeleton served by `vite preview` on port 4173: no form (3); export answers 500, no backend behind `/api` (1) |

## First complete run (before any fix)

## Final run (phase 6)
