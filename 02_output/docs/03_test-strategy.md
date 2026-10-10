# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

| Level | Component | Location | Runner | Interface | Frozen |
|---|---|---|---|---|---|
| Acceptance | backend | `backend/src/test/java/si/konferenca/registration/acceptance/` | JUnit 6 via `./mvnw verify` (Surefire) | HTTP against the started application (`@SpringBootTest` random port), Testcontainers PostgreSQL and Mailpit, a local HTTP server as the mocked reCAPTCHA endpoint (production code path), JSON copy directory, Mailpit HTTP API, PostgreSQL via JDBC for storage checks | yes |
| Acceptance | frontend | `frontend/tests/acceptance/` | Vitest + Testing Library (`npm test`) | rendered UI (`App`), `fetch` mocked at the network boundary according to `02_contracts/openapi.yaml` | yes |
| End-to-end | frontend + backend | `frontend/e2e/` | Playwright Chromium (`npm run test:e2e`) | browser against the local compose stack (frontend 127.0.0.1:8081), Mailpit API 127.0.0.1:8025, export over HTTP with organizer credentials from the environment | yes |
| Architecture | backend | `backend/src/test/java/si/konferenca/registration/architecture/` | JUnit + ArchUnit | compiled classes | no (phase 5) |
| Unit / integration | both | any other test path | JUnit / Vitest | implementation | no (phase 5) |

Request isolation in backend acceptance tests: each request carries its own `X-Forwarded-For` client address (the backend trusts forwarded headers from loopback, spec section 8), so rate limits apply per test; organizer requests add `X-Forwarded-Proto: https` unless the test checks SR-06.

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`@SpringBootApplication`, no logic); allowed by `quality-requirements.md` |
| `frontend/src/main.tsx` | bootstrap file that only mounts the app; allowed by `quality-requirements.md` |
| `frontend/src/setupTests.ts` | test setup (imports jest-dom matchers), not shipped |
| `frontend/src/**/*.d.ts` | type declarations, no runtime code |

## First complete run (before any fix)

## Final run (phase 6)
