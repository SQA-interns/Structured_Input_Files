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

Phase 5, after the unit, integration and architecture tests were written; all levels, one run each (`logs/phase5-first-run-*.log`).

| Suite | Level | Passed | Failed |
|---|---|---|---|
| backend (`./mvnw -B verify`) | acceptance | 81 | 0 |
| backend | architecture (ArchUnit) | 7 | 0 |
| backend | integration (`JpaRegistrationStoreIntegrationTest`) | 0 | 3 |
| backend | unit | 88 | 1 |
| frontend (`npx vitest run`) | acceptance | 12 | 0 |
| frontend | unit / component | 47 | 0 |
| e2e (`npx playwright test`, compose stack) | e2e | 5 | 0 |
| **total** | | **240** | **4** |

Classification of the failures:

| Test | Class | Cause | Action |
|---|---|---|---|
| `OptionCatalogLoaderTest.rejectsInvalidFilesNamingTheProblem` (1 error for the whole parameterised source) | Defect in a non-frozen test | `@CsvSource` treats the apostrophe as quote character; the expected messages contain apostrophes | quote character set to a backtick; the source now yields 17 cases |
| `JpaRegistrationStoreIntegrationTest` (3 errors) | Defect in a non-frozen test | `webEnvironment = NONE` cannot create the servlet `SecurityFilterChain` | `webEnvironment = MOCK` |

No implementation defect and no frozen-test problem was found by the first run.

### Phase 5 gate run

Backend 196 passed, 0 failed (acceptance 81, architecture 7, integration 3, unit 105); frontend 59 passed (acceptance 12, unit/component 47); e2e 5 passed (`logs/phase5-gate-*.log`, `logs/phase5-first-run-e2e.log`).

## Final run (phase 6)

On the final code (after F-01, F-07, F-08, F-10 and the Vitest 4.1.11 change):

| Suite | Level | Passed | Failed |
|---|---|---|---|
| backend (`./mvnw -B verify`, `logs/phase6-final-backend.log`) | acceptance | 81 | 0 |
| backend | architecture | 7 | 0 |
| backend | integration | 3 | 0 |
| backend | unit | 106 | 0 |
| frontend (`npx vitest run`, `logs/phase6-final-frontend.log`) | acceptance | 12 | 0 |
| frontend | unit / component | 49 | 0 |
| e2e (`npx playwright test` against the rebuilt compose stack, `logs/phase6-final-e2e.log`) | e2e | 5 | 0 |
| **total** | | **263** | **0** |

The e2e run preceded the frontend test-only change of F-10 (no production code changed afterwards).

| Measure | Component | unit | nonUnit | all | Tool |
|---|---|---|---|---|---|
| Line coverage | backend | 74.9 % (746/996) | 90.7 % (903/996) | 97.7 % (973/996) | JaCoCo 0.8.12 |
| Branch coverage | backend | 86.7 % (274/316) | 69.0 % (218/316) | 93.0 % (294/316) | JaCoCo 0.8.12 |
| Line coverage | frontend | 97.6 % (166/170) | 92.4 % (157/170) | 100 % (170/170) | Vitest 4.1.11 v8 |
| Branch coverage | frontend | 92.6 % (112/121) | 81.0 % (98/121) | 97.5 % (118/121) | Vitest 4.1.11 v8 |

Frontend non-unit coverage covers the Vitest acceptance tests; the Playwright e2e tests are not instrumented.

| Mutation | score | scoreCovered | killed | total | covered | tests | tool |
|---|---|---|---|---|---|---|---|
| backend | 95.4 % | 98.2 % | 374 | 392 | 381 | all 197 JUnit tests | PIT 1.30.0 |
| frontend | 84.7 % | 84.7 % | 371 | 438 | 438 | all 61 Vitest tests (no e2e) | Stryker 10.0.0 + Vitest 4.1.11 |

Code size (cloc 2.10 image, reports 1.98): backend production 2296, test 3241; frontend production 607, test 998 lines of code (`logs/phase6-cloc.txt`). Duplication: PMD CPD (backend) and jscpd (frontend) report 0 clones.
