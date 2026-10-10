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

Test names carry their AC id: JUnit methods are named `ac_nnn_nn_…` and shown as `AC-nnn-nn …` (`AcIdDisplayNames`), parameterised cases name the AC explicitly; Vitest and Playwright titles start with the AC ids.

The e2e files are not type-checked by `tsc` (they use Node APIs and `@types/node` is not in `tech-stack.md`); Playwright transpiles them and ESLint and Prettier check them.

### AC coverage of the frozen tests

| AC | Backend acceptance | Frontend acceptance | e2e |
|---|---|---|---|
| AC-001-01 | `ExternalRegistrationAcceptanceTest` | `external-form.test.tsx` | |
| AC-001-02 | `ExternalRegistrationAcceptanceTest` | | `external-registration.spec.ts` |
| AC-001-03, -04, -05, -07, -08, -10, -11 | `ExternalRegistrationAcceptanceTest` | | |
| AC-001-06 | `ExternalRegistrationAcceptanceTest` | | `external-registration.spec.ts` |
| AC-001-09 | `ExternalRegistrationAcceptanceTest` (mocked verification endpoint) | `external-form.test.tsx` | |
| AC-001-12 | | `external-form.test.tsx` | `external-registration.spec.ts` |
| AC-001-13 | `RateLimitAcceptanceTest` | | |
| AC-002-01 | `StudentRegistrationAcceptanceTest` | `student-form.test.tsx` | `student-registration.spec.ts` |
| AC-002-02 | `StudentRegistrationAcceptanceTest` | | `student-registration.spec.ts` |
| AC-002-03, AC-002-04 | `StudentRegistrationAcceptanceTest` | `student-form.test.tsx` | |
| AC-003-01..03 | `ConferenceOptionsAcceptanceTest` | | |
| AC-004-01 | `RegistrationConfirmationAcceptanceTest` | `confirmation.test.tsx` | `external-registration.spec.ts` |
| AC-004-02 | `RegistrationConfirmationAcceptanceTest` | `confirmation.test.tsx` | |
| AC-005-01, AC-005-02 | `RegistrationStorageAcceptanceTest` | | |
| AC-005-03 | `DatabaseUnavailableAcceptanceTest` | | |
| AC-006-01, AC-006-02 | `ParticipantEmailAcceptanceTest` | | AC-006-01 in both e2e registration specs |
| AC-006-03 | `MailUnavailableAcceptanceTest` | | |
| AC-007-01, AC-007-02 | `OrganizerNotificationAcceptanceTest` | | AC-007-01 in `external-registration.spec.ts` |
| AC-008-01 | `RegistrationExportAcceptanceTest` | | `export.spec.ts`, `external-registration.spec.ts` |
| AC-008-02 | `RegistrationExportAcceptanceTest` | | `export.spec.ts` |
| AC-008-03, AC-008-05 | `RegistrationExportAcceptanceTest` | | |
| AC-008-04 | `RateLimitAcceptanceTest` | | |

### Phase 3 run against the bootstrap skeleton (before the freeze)

| Suite | Command | Passed | Failed | Reason | Log |
|---|---|---|---|---|---|
| Backend acceptance | `./mvnw -B test` | 3 | 78 | The skeleton has no API: Spring Security's default chain answers every `/api` request with 401, so status, body, storage and email assertions fail; AC-003-03 fails because the skeleton starts despite an invalid options file. | `logs/phase3-backend-acceptance-run.log` |
| Frontend acceptance | `npx vitest run` | 0 | 12 | The skeleton page has no registration type choice or form (`radiogroup "Registration type"` not found). | `logs/phase3-frontend-acceptance-run.log` |
| End-to-end | `npx playwright test` against the skeleton build served by `vite preview` on 127.0.0.1:8081 | 0 | 5 | `/api/form-config` is not served (500 from the preview proxy, no backend) and the page has no form. | `logs/phase3-e2e-run.log` |

Tests that pass only because bootstrap code already provides the behaviour (Spring Security's default HTTP Basic chain refuses unauthenticated requests with 401 and `WWW-Authenticate: Basic`):

- `RegistrationExportAcceptanceTest.ac_008_02_missingCredentialsAreRefused`
- `RegistrationExportAcceptanceTest.ac_008_02_wrongPasswordIsRefused`
- `RegistrationExportAcceptanceTest.ac_008_02_wrongUsernameIsRefused`

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
