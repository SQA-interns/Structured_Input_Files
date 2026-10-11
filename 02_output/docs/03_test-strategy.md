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

After the freeze, D-12 / D-13 (human-approved) restructured the property registration of four frozen classes without changing any assertion; the manifest was regenerated in its own commit.

## First complete run (before any fix)

Phase 5 start, 2026-10-11, on the phase 4 code (logs `phase5-first-run-*.log`): backend 93 passed / 0 failed (acceptance), frontend 7 / 0 (acceptance), e2e 2 / 0 against the compose stack. Nothing to classify.

## Phase 5 results

Tests at the end of phase 5 (`phase5-final-backend.log`, Vitest, `phase5-final-e2e.log`):

| Component | unit | integration | architecture | acceptance | e2e | Failed |
|---|---|---|---|---|---|---|
| backend | 87 | 3 | 6 | 93 | – | 0 |
| frontend | 18 | – | – | 7 | 2 | 0 |

Defect found while writing phase 5 tests (implementation defect, code fixed in commit `fix: give stored option and consent rows value equality`): `JpaRegistrationStore.findAll` fetch-joins options and consents; the embeddable rows had identity equality, so a registration with three options was read back with its consent three times (visible in the export's Consents cell). No frozen test covered it. The defect in a non-frozen test fixed in the same phase: none.

### Coverage (measured once in phase 5, before the survivor tests)

Scope: production code as in "Measurement exclusions". Backend JaCoCo 0.8.12 (`target/jacoco.exec` deleted before each level), frontend Vitest 4.1.11 v8 provider.

| Component | Level | Line | Branch |
|---|---|---|---|
| backend | unit (85 tests incl. architecture) | 70.2 % (576/820) | 88.2 % (216/245) |
| backend | nonUnit (93 acceptance) | 91.2 % (748/820) | 76.7 % (188/245) |
| backend | all (178) | 96.7 % (793/820) | 94.3 % (231/245) |
| frontend | unit (16) | 80.0 % (112/140) | 72.0 % (72/100) |
| frontend | nonUnit (7 acceptance; e2e not instrumented) | 84.3 % (118/140) | 77.0 % (77/100) |
| frontend | all (23) | 89.3 % (125/140) | 93.0 % (93/100) |

After the survivor tests and the fix, backend all: 99.0 % line (823/831), 93.1 % branch (243/261).

### Mutation score (one full run per component)

| Component | Tool | Scope | Tests | Mutants | Killed (incl. timeout) | No coverage | Survived | score | scoreCovered |
|---|---|---|---|---|---|---|---|---|---|
| backend | PIT 1.30.0 + junit5 plugin 1.2.3, default mutators | `si.konferenca.registration.*` minus `RegistrationApplication` | full suite: unit, architecture, acceptance (178) | 307 | 282 | 13 | 12 | 91.9 % | 95.9 % (282/294) |
| frontend | Stryker 10.0.0, vitest runner | `src/**/*.{ts,tsx,js,jsx}` minus tests, `*.d.ts`, `main.tsx`, `test-setup.ts` | Vitest suite (23); e2e excluded, D-15 | 354 | 275 (267 + 8) | 20 | 58 (+1 runtime error) | 77.7 % | 82.6 % |

Sanity check before each full run: PIT on `RegistrationValidator` with the domain tests killed 100 %; Stryker on `validation.ts` 96.6 % — both tools activate mutants.

### Surviving and uncovered mutants in validation, security, persistence and business-rule code

Backend: all production code is in that scope, so every survivor and uncovered mutant is classified.

| Mutant(s) | Classification |
|---|---|
| `FixedWindowRateLimiter` eviction predicate (4: boundary, negation, true, subtraction) | killed by new test `SurvivingMutantsApiTest.limiterEvictsExactlyTheWindowsThatExpired` |
| `MailRegistrationNotifier.close` shutdown removed | killed by `MailRegistrationNotifierTest.closeStopsTheBackgroundExecutor` |
| `ApiExceptionHandler.rootCauseType` (3), `mediaType`, `method`, `notFound`, `unexpected` null returns (4, no coverage) | killed by `SurvivingMutantsApiTest.handlersAnswerWithTheErrorShape` and `rootCauseTypeFollowsTheCauseChain` |
| `ErrorResponse.of` null return | killed by `handlersAnswerWithTheErrorShape` |
| `SecurityConfig.cors` (4, no coverage) | killed by `CorsConfigTest` |
| `SecurityConfig` access-denied handler (no coverage) | killed by `PersistenceAndSecurityIntegrationTest.authenticatedOrganizerIsDeniedEverythingButTheExport` |
| `RecaptchaHttpVerifier` interrupt flag (no coverage) | killed by `CaptchaVerifierTest.interruptedCallIsUnavailableAndKeepsTheInterruptFlag` |
| `RequestLimitFilter` cached stream `isFinished`/`isReady` (3, no coverage) | killed by `SurvivingMutantsApiTest.cachedBodyStreamReportsReadinessAndEnd` |
| `ConsentRow.consentText`, `ConsentRow.givenAt`, `OptionRow.optionId` getters | killed by `PersistenceAndSecurityIntegrationTest.storedRegistrationReadsBackUnchanged` (which also exposed the defect above) |

Frontend: the validation code is `validation.ts`.

| Mutant | Classification |
|---|---|
| `CAPTCHA_REQUIRED` text emptied | killed by `validation.test.ts` "uses the contract wording for the captcha error" |
| email regex without `$` | killed by "rejects text after a valid address" |

The other frontend survivors are in presentation code (labels, loading text, the reCAPTCHA loader only reachable with Google's script, request headers); the score is recorded only (`quality-requirements.md`). They are reviewed as possible findings in phase 6. Because code and tests changed after the runs, both components are re-measured once in phase 6.

## Final run (phase 6)

Final run after the last fix (2026-10-11): backend 189 passed (87 unit, 6 architecture, 3 integration, 93 acceptance), frontend 27 passed (20 unit, 7 acceptance), e2e 2 passed; 0 failed, 0 skipped. Coverage and mutation re-measured once (both components changed after phase 5): backend PIT 320/320 (100 %), frontend Stryker 85.9 % (304/354); details in `docs/06_verification-report.md`.
