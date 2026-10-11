# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Interface | Frozen |
|---|---|---|---|---|---|
| acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` (tests and `support/` helpers), `backend/src/test/resources/acceptance/` | JUnit Jupiter via Surefire (`./mvnw verify`) | real HTTP against the application on a random port; PostgreSQL 16.15 and Mailpit v1.31.1 in Testcontainers; JDBC and the JSON copy directory only to observe results; a JDK HTTP server as mocked reCAPTCHA endpoint | yes |
| acceptance | frontend | `frontend/tests/acceptance/` | Vitest (jsdom) | the rendered `App` component, `fetch` stubbed per `openapi.json` | yes |
| e2e | frontend + backend | `frontend/tests/e2e/` | Playwright (Chromium) | browser against the running local stack (`docker compose up`), Mailpit API, `/api/export` | yes |
| unit, integration, architecture | both | any other test path | Surefire / Vitest | internals | no (phase 5) |

Requirements without their own AC that still get frozen acceptance tests: SR-01/DoD-P05 (production reCAPTCHA path against a mocked endpoint), SR-02 (production start refused), SR-03 (rate and size limits), SB-10 (security headers), NFR-04 (health and readiness), NFR-01 (end-to-end with Slovenian characters).

Test configuration for backend acceptance tests: properties set through `@DynamicPropertySource` using the names in specification section 3 (`app.environment=test`, `app.recaptcha.test-mode=true`, `app.conference.config-file=<temp copy of acceptance/conference-test.json>`, `app.json-copy-dir=<temp dir>`, organizer test credentials, `app.organizer.emails`, rate limits raised except in the limit test).

## Measurement exclusions

Decided before any test was written (`measurement-scope.md`).

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`@SpringBootApplication`, no logic); listed exclusion in `quality-requirements.md` |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app; listed exclusion |
| `frontend/src/**/*.d.ts` | type declarations; listed exclusion |
| `frontend/src/test-setup.ts` | test setup, not production code |

No generated code exists in either component. Nothing else is excluded.

## First complete run (before any fix)

## Final run (phase 6)
