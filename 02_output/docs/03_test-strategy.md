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

## Final run (phase 6)

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
