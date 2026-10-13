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

Phase 5, on the final phase 4 code, before any unit test or fix (logs `out/logs/05_first-run-*.log`):

| Suite | Passed | Failed | Coverage (that suite alone) |
|---|---|---|---|
| backend acceptance (JUnit, Testcontainers) | 84 | 0 | lines 87.85 % (774/881), branches 68.33 % (192/281), JaCoCo 0.8.12 |
| frontend acceptance (Vitest) | 10 | 0 | lines 74.85 % (125/167), branches 69.23 % (72/104), @vitest/coverage-v8 4.1.11 |
| end-to-end (Playwright, local stack) | 5 | 0 | not instrumented (containers built from source) |

No failure, so nothing to classify. The defect E2E-01 found in phase 4 (attachment line endings) was fixed in phase 4 (commit `fix: keep JSON attachment byte-equal to stored copy`).

### Quality gate (phase 5)

| Component | Line (all) | Mutation (adjusted) | Gate | Survivor rounds | Tests at first full mutation run | Tests at the end |
|---|---|---|---|---|---|---|
| backend | 99.32 % ≥ 95 % | 95.72 % ≥ 90 % | met | 0 (first full run met it) | 216 | 216 |
| frontend | 100 % ≥ 95 % | first full run 89.66 % (364/406); after round 1 (targeted re-run of api, App, Recaptcha, RegistrationForm, validation) and the confirming full run 94.83 % (385/406) ≥ 90 % | met | 1 | 56 | 69 |

Mutation tooling check: both tools kill mutants in plainly covered code (backend 96 % test strength, frontend 89.7 % on the first run against 100 % line coverage), so mutants are activated.

### Surviving mutants (classified; none counted as equivalent)

Backend (PIT, 16; security and validation ones classified individually, all Low: the behaviour is checked elsewhere or the effect is not observable):

| Class.method:line | Mutation | Classification |
|---|---|---|
| domain.RegistrationValidator.options:124 | return empty list | Low: the returned list is replaced only on the error path (field error already set); a valid submission with options is checked by acceptance tests through the stored options, but this mutant keeps errors identical |
| security.RateLimitFilter.doFilterInternal:36,40,41 (4) | pass-through call removed, window arithmetic, cleanup boundary | Low: unit tests do not assert the chain call on unlimited paths, nor the exact cleanup boundary; limits themselves are tested |
| security.BodySizeLimitFilter.doFilterInternal:37,42 (2) | boundary of the size checks | Low: limit+1 vs limit boundaries partly covered; off-by-one on 16 KiB has no security impact |
| security.RequestLimitConfiguration (4) | URL pattern / order not set | Low: filter wiring; with no pattern the filter applies to all paths (still limited); order change keeps both filters before the controllers |
| security.SecurityConfiguration:37 / :43 (2) | `WWW-Authenticate` header not set; access-denied JSON (no coverage) | Low: the 401 status and absence of data are tested (AC-008-02); the header and the 403 handler for authenticated-but-denied requests are not asserted |
| security.ErrorResponses.write:23 | charset not set | Low: messages are ASCII |
| mail.MailNotifier:98, :106 (2) | attachment disposition not set; `validate()` removed | Low: the attachment is still recognised by file name; the address is still parsed strictly and compared to the input |

Frontend (Stryker, 21): api.ts 7 (body/field-error type checks whose mutants end in the same `failed` result through the surrounding `catch`), App.tsx 4 (state kind string and effect cleanup, not observable), Recaptcha.tsx 7 (widget lifecycle guards and effect dependencies), RegistrationForm.tsx 3 (initial option list and `type === null` guards already covered by validation). All Low; none is in validation code (validation.ts 100 %).

## Final run (phase 6)

No production code or test changed after phase 5, so coverage and mutation were not re-measured. Final run on the final code: backend 216 passed / 0 failed (unit 127, acceptance 84, architecture 5), frontend 69 / 0 (unit 59, acceptance 10), end-to-end 5 / 0 against the rebuilt images (`out/logs/06_*-final.log`).

## Test measures (phase 5, updated in phase 6 only for a component that changed)

<!-- Filled in phase 5 from the runners and tools; copied into the run summary in phase 7. Definitions: general/quality/measurement-scope.md. Level rows: tests passed / failed. unit = coverage of the unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 84 / 0 | — | — | — | JUnit 6, Testcontainers PostgreSQL + Mailpit |
| backend | integration | 0 / 0 (the acceptance tests are the integration level) | — | — | — | — |
| backend | architecture | 5 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 127 / 0 | 92.88 % (822/885) | 99.65 % (284/285) | — | JUnit 6, Mockito, JaCoCo 0.8.12 |
| backend | non-unit | 84 / 0 | 87.68 % (776/885) | 68.07 % (194/285) | — | JaCoCo 0.8.12 |
| backend | all levels | 216 / 0 | 99.32 % (879/885) | 99.65 % (284/285) | 95.72 % raw = adjusted (358 killed of 374; 15 survived, 1 no coverage; 0 equivalent), scope `si.konferenca.registration.**` minus `RegistrationApplication`, tests: all JUnit levels (D-14) | PIT 1.30.0 + junit5 plugin 1.2.3 |
| frontend | acceptance | 10 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 59 / 0 | 100 % (167/167) | 99.03 % (103/104) | — | Vitest, @vitest/coverage-v8 4.1.11 |
| frontend | non-unit | 10 / 0 | 74.85 % (125/167) | 69.23 % (72/104) | — | @vitest/coverage-v8 4.1.11 |
| frontend | all levels | 69 / 0 | 100 % (167/167) | 99.03 % (103/104) | 94.83 % raw = adjusted (385 killed of 406; 21 survived; 0 equivalent), scope `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts`, tests; tests: all Vitest levels (D-14) | Stryker 10.0.0, vitest-runner |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 against `docker compose` |
