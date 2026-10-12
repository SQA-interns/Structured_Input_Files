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
