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

## Phase 3 run against the bootstrap skeleton (before the freeze)

Run on 2026-10-11 against the phase 0 skeletons (no production behaviour). Logs: `out/logs/phase3-backend-acceptance-run.log`, `phase3-frontend-acceptance-run.log`, `phase3-e2e-run.log`.

| Suite | Tests | Failed | Passed | Reason for the failures |
|---|---|---|---|---|
| backend acceptance (`./mvnw verify`) | 93 | 90 | 3 | Every `/api` request answers 401 from the default Spring Security configuration because no endpoint exists (84); invalid configurations and unsafe production settings do not stop the start because no checks exist (6). No build, container or harness errors. |
| frontend acceptance (`npx vitest run`) | 7 | 7 | 0 | The skeleton page has no conference heading, form or confirmation. |
| e2e (`npx playwright test` against the Vite dev server, `E2E_BASE_URL=http://127.0.0.1:5173`; the compose stack does not exist yet) | 2 | 2 | 0 | The registration-type radios and fields do not exist. |

Tests that pass only because the bootstrap already provides the behaviour:

| Test | Why it passes on the skeleton |
|---|---|
| `HttpHardeningAcceptanceTest.nfr_04_healthAndReadinessAreReported` | Spring Boot Actuator in the skeleton already exposes liveness and readiness. |
| `ConferenceOptionsAcceptanceTest.ac_003_03_validConfigurationStarts` | control case: a valid configuration starts (the skeleton starts with any configuration). |
| `ProductionStartupAcceptanceTest.sr_02_completeProductionConfigurationStarts` | control case: a complete production configuration starts. |

Self-check before the freeze (skill step 5): every helper returns what the assertions expect (JDBC timestamps only checked for presence; Mailpit lists read leniently for empty Cc/Bcc; JSON numbers read as Integer); the workbook text reader of the end-to-end test was checked against a workbook written by Apache POI 5.5.1 with Slovenian text; each rejection test also checks that neither the unique email nor the unique last name reached the database or a JSON copy; startup-refusal tests have a control that must start with the same settings.

Frozen: 102 tests (backend 93, frontend acceptance 7, e2e 2) for 35 AC; manifest `docs/03_acceptance-manifest.sha256`.

## First complete run (before any fix)

## Final run (phase 6)
