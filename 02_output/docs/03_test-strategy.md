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

## First complete run (before any fix)

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
