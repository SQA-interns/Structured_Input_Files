# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: `dependency-check-maven` 12.1.0 Sonatype OSS Index analyser answers HTTP 401 (needs credentials not listed in `secrets.env.example`); preflight step 6. Log: `out/logs/00_dependency-check.log` (first run).
- Options: 1. disable the OSS Index analyser and keep the NVD analyser (default); 2. ask for OSS Index credentials.
- Human response: none
- Resolution: 1 (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`); scan re-run and completed with the NVD analyser.

## D-02: PA-01 CVE-2025-7962 on angus-activation
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High) on `org.eclipse.angus:angus-activation` 2.0.3; shipped `angus-mail` is 2.0.5. Matches `project/00_setup/pre-approved.md` PA-01.
- Options: 1. apply PA-01 (default).
- Human response: none (pre-approved)
- Resolution: 1 — Low, false positive; suppressed for that artifact only in `backend/dependency-check-suppressions.xml`.

## D-03: PA-02 CVE-2025-15104 on hibernate-validator
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-15104 (CVSS 5.3) on `hibernate-validator` 9.1.3.Final via CPE `validator:validator`. Matches PA-02.
- Options: 1. apply PA-02 (default).
- Human response: none (pre-approved)
- Resolution: 1 — Low, false positive; suppressed for that artifact only.

## D-04: PA-03 npm audit moderate advisories via Stryker
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 Moderate (`qs` 6.15.1, `typed-rest-client`) reached only through `@stryker-mutator/core`; `npm audit --omit=dev` reports 0. Matches PA-03.
- Options: 1. apply PA-03 (default).
- Human response: none (pre-approved)
- Resolution: 1 — Medium, accepted (dev tooling, not in the bundle).

## D-05: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run aldanial/cloc:2.10 --version` prints `1.98`; the pinned tag resolves and runs (tech-stack.md rule: pin runs but reports another version).
- Options: 1. keep the pinned tag `aldanial/cloc:2.10`, the pin stays authoritative (default); 2. change the pin (blocking).
- Human response: none
- Resolution: 1

## D-06: dev-only @types/node 24.19.2 added to the frontend
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: non-blocking
- Trigger: `tsc --noEmit` needs Node type definitions for `vite.config.ts`, `playwright.config.ts` and the end-to-end tests (they read `process.env` and query the local stack); no such package in `tech-stack.md`.
- Options: 1. add `@types/node` 24.19.2 (MIT, matches Node 24) as an exact dev dependency (default); 2. exclude config and e2e files from type checking.
- Human response: none
- Resolution: 1 — scanned with `npm audit` like any other dependency.

## D-07: Dependency-Check matches PostgreSQL server CVEs on testcontainers-postgresql
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: blocking
- Trigger: With test scope included, Dependency-Check maps `org.testcontainers:testcontainers-postgresql` 2.0.5 (test scope) to CPE `cpe:2.3:a:postgresql:postgresql:2.0.5` and reports 45 PostgreSQL **server** CVEs (4 Critical, e.g. CVE-2019-10211 "Postgresql Windows installer…"; 21 High, e.g. CVE-2026-6473 "integer wraparound in PostgreSQL server"). The jar is a Java test helper that starts a container; it contains no PostgreSQL server code. The database actually used is the `postgres:16.15-alpine` image, not this jar. Evidence: `out/logs/00_dependency-check-report.json`.
- Options: 1. classify Low, false positive (CPE name collision); suppress these CVEs for `testcontainers-postgresql` only (proposed default); 2. keep them as Critical/High and stop the run; 3. replace Testcontainers (tech-stack change).
- Human response: "apply proposed default" (2026-10-12T19:15Z)
- Resolution: 1 — Low, false positive (CPE name collision); suppressed for `testcontainers-postgresql` only.

## D-08: Dependency-Check CVEs in httpclient5/httpcore5 shaded in docker-java-transport-zerodep
- Timestamp: 2026-10-12T19:12:23Z
- Phase: 0
- Type: blocking
- Trigger: `com.github.docker-java:docker-java-transport-zerodep` 3.7.1 (transitive, test scope only, via Testcontainers 2.0.5) shades `httpclient5` 5.5.1 and `httpcore5` 5.3.6: CVE-2026-71290 (Critical 9.1, TLS hostname verification in the async client), CVE-2026-54399 and CVE-2026-54428 (High 7.5, HTTP/1.1 and HTTP/2 DoS by a malicious server), CVE-2026-64607 (Medium 5.3). The library is used only by tests to talk to the local Docker daemon over a local named pipe/socket; it is not in the shipped backend jar or image, and no untrusted server or TLS peer is contacted. Shaded code cannot be overridden by a version property.
- Options: 1. classify Low (test-only, not shipped, no reachable attacker path); suppress for this artifact only; record as finding (proposed default); 2. keep them as Critical/High and stop the run; 3. change the Testcontainers version (tech-stack change).
- Human response: "apply proposed default" (2026-10-12T19:15Z)
- Resolution: 1 — Low (test-only, not shipped, no reachable attacker path); suppressed for `docker-java-transport-zerodep` only; recorded as a finding for phase 6.

## D-09: Editorial defects in requirement inputs
- Timestamp: 2026-10-12T19:17:00Z
- Phase: 1
- Type: non-blocking
- Trigger: `project/01_requirements/scope.md` starts with a stray line that repeats the OQ answers before the `# Scope` heading; AC-006-03 cites `OQ-03, OQ-03`.
- Options: 1. treat as editorial; the `## Open questions` table is authoritative; inputs unchanged (default).
- Human response: none
- Resolution: 1

## D-10: Status for organizer export over plain HTTP from a non-local client (AC-008-04)
- Timestamp: 2026-10-12T19:17:00Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-008-04 requires refusal but names no status.
- Options: 1. HTTP 403 with a generic error body and no data, checked before credentials are evaluated (default, conservative); 2. redirect to HTTPS (may leak credentials on retry).
- Human response: none
- Resolution: pending review (option 1)

## D-11: Error response when the JSON copy cannot be written (AC-005-03)
- Timestamp: 2026-10-12T19:17:00Z
- Phase: 1
- Type: non-blocking
- Trigger: AC-005-03 requires "an error (not 201)" without a status.
- Options: 1. HTTP 500 with a generic message and no internals, database transaction rolled back (default); 2. HTTP 503.
- Human response: none
- Resolution: pending review (option 1)

## D-12: Fields of the other registration type in a submission
- Timestamp: 2026-10-12T19:21:27Z
- Phase: 2
- Type: non-blocking
- Trigger: BR-01 fixes the fields per type; the inputs do not say what happens when, for example, an EXTERNAL submission also carries `studentId`.
- Options: 1. reject with a field error on that field (default, conservative: nothing outside the type's fields is ever stored); 2. silently ignore the extra field.
- Human response: none
- Resolution: pending review (option 1)

## D-13: Deployment files written in phase 3 as end-to-end harness
- Timestamp: 2026-10-12T19:31:50Z
- Phase: 3
- Type: non-blocking
- Trigger: the phase 3 gate needs every end-to-end test to fail for a behavioural reason, which needs the local stack (`e2e-scenarios.md`: `docker compose up`); phase 0 created only the component skeletons.
- Options: 1. write `docker-compose.yml`, both Dockerfiles and the frontend `nginx.conf` in phase 3 as harness, no application behaviour; the skeleton backend does not start (no datasource wiring yet) and the skeleton frontend is served, so E2E tests fail on missing behaviour (default); 2. leave the stack to phase 4 and accept connection errors in phase 3.
- Human response: none
- Resolution: 1

## D-14: SpotBugs exposure warnings excluded for the backend
- Timestamp: 2026-10-12T20:03:02Z
- Phase: 4
- Type: non-blocking
- Trigger: SpotBugs 4.10.4.1 (effort Max, threshold Medium) reports 30 EI_EXPOSE_REP/EI_EXPOSE_REP2 warnings on records that carry lists (request, command, error body, configuration) and on Spring-injected collaborators held in final fields. No mutable state is shared with untrusted code; all lists come from Jackson, configuration binding or `List.copyOf`.
- Options: 1. exclude the two exposure patterns in `backend/spotbugs-exclude.xml` and classify them Low (default); 2. add defensive copies to every record and constructor.
- Human response: none
- Resolution: 1 (the other finding, CT_CONSTRUCTOR_THROW in `StartupGuard`, was fixed in code)

## D-15: Two additional allowed package dependencies in the backend architecture
- Timestamp: 2026-10-12T20:04:03Z
- Phase: 4
- Type: non-blocking
- Trigger: the layer table of `docs/02_specification.md` section 2 omitted two dependencies the build needs: `web` maps the stored `Registration` entity to the 201 response, and `conference` uses `domain.RegistrationType` for `availableTo`.
- Options: 1. allow `web → domain` (entity types; repositories stay forbidden by rule f) and `conference → domain`, update the table, and check exactly that with ArchUnit (default); 2. add mapping types in `service` and a separate type enum in `conference`.
- Human response: none
- Resolution: 1

## D-16: End-to-end tests are not part of the mutation runs
- Timestamp: 2026-10-12T20:24:47Z
- Phase: 5
- Type: non-blocking
- Trigger: `measurement-scope.md` asks for each component's full automated suite in the mutation run. The 5 end-to-end tests run in a browser against Docker images built from the code; PIT and Stryker mutate code in-process (bytecode in the test JVM, sources in a sandbox) and cannot rebuild and restart the images for every mutant.
- Options: 1. mutation runs use every other level: backend unit, integration, acceptance and architecture tests (JUnit via PIT); frontend unit and acceptance tests (Vitest via Stryker); the e2e tests stay outside, recorded in `tests` of the mutation record (default); 2. rebuild the stack per mutant (infeasible: hours per mutant set).
- Human response: none
- Resolution: 1
