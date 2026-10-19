# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker daemon not running
- Timestamp: 2026-10-19T12:31Z
- Phase: 0
- Type: blocking
- Trigger: preflight step 2 and 3 (`project/00_setup/tech-stack.md` platform `docker`, `environments.md` test and local). `docker version` reports client 29.8.1 but cannot reach the engine (`npipe:////./pipe/dockerDesktopLinuxEngine` not found); confirmed a second way with `docker info` (same error). Docker Compose v5.5.1 is present. Testcontainers, the local stack and the semgrep, gitleaks and cloc images all need the engine.
- Options: 1. (proposed default) the human starts Docker Desktop / the Docker Engine 29.8.1 and the agent re-runs the Docker checks; 2. stop the run.
- Human response: "Docker Desktop is started now, continue." (2026-10-19T12:32Z)
- Resolution: option 1; re-check passed: `docker version` server 29.8.1, every pinned image pulled and run

## D-02: npm audit moderate advisories through Stryker (PA-03)
- Timestamp: 2026-10-19T12:31Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 moderate advisories (`qs` 6.15.1 via `typed-rest-client` 2.3.1, reached only through `@stryker-mutator/core` 10.0.0); `npm audit --omit=dev` reports 0. Evidence matches `project/00_setup/pre-approved.md` PA-03. Log: `out/logs/phase0-npm-audit.json`.
- Options: 1. (proposed default) apply PA-03: accept, record as Medium, accepted.
- Human response: none (pre-approved, PA-03)
- Resolution: option 1, Medium, accepted

## D-03: cloc image reports version 1.98
- Timestamp: 2026-10-19T12:34Z
- Phase: 0
- Type: non-blocking
- Trigger: `tech-stack.md` pins `aldanial/cloc:2.10`; the tag resolves and the tool runs, but `cloc --version` in the image prints 1.98 (checked twice). Rule in `tech-stack.md`: a pin that runs but reports another version is non-blocking.
- Options: 1. (proposed default) keep the pinned tag `2.10` as authoritative and record the reported version; 2. ask for another tag.
- Human response: none
- Resolution: option 1

## D-04: OWASP Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-19T12:52Z
- Phase: 0
- Type: non-blocking
- Trigger: preflight step 6. The Sonatype OSS Index analyser answers HTTP 401 Unauthorized for every artifact (`out/logs/phase0-dependency-check.log`); it needs credentials that `secrets.env.example` does not list. The NVD analyser works with `NVD_API_KEY`.
- Options: 1. (proposed default) disable the OSS Index analyser (`ossindexAnalyzerEnabled=false` in `backend/pom.xml`) and re-run with the remaining analysers; 2. ask for OSS Index credentials.
- Human response: none
- Resolution: option 1; re-run completed (`out/logs/phase0-dependency-check-rerun.log`)

## D-05: Dependency-Check findings on angus-activation and hibernate-validator (PA-01, PA-02)
- Timestamp: 2026-10-19T12:55Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High) on `angus-activation` 2.0.3 and CVE-2025-15104 (CVSS 5.3, Medium) on `hibernate-validator` 9.1.3.Final. Evidence: the shipped `angus-mail` is 2.0.5 (`out/logs/phase0-backend-dependency-tree.log`), matching PA-01; CVE-2025-15104 is the Nu Html Checker CPE name collision, matching PA-02 (`project/00_setup/pre-approved.md`).
- Options: 1. (proposed default) apply PA-01 and PA-02: classify both Low, false positive, and suppress each for its one artifact only (`backend/dependency-check-suppressions.xml`).
- Human response: none (pre-approved, PA-01, PA-02)
- Resolution: option 1; re-scan with `failBuildOnCVSS=7`: 0 open, 2 suppressed

## D-06: Stray answer fragment in business-rules.md
- Timestamp: 2026-10-19T12:36Z
- Phase: 1
- Type: non-blocking
- Trigger: `project/01_requirements/business-rules.md` ends with a header-less table fragment repeating the OQ-01..06 answers (`docs/01_requirements-check.md`, G-1). It does not contradict `scope.md`.
- Options: 1. (proposed default) treat the answers in `scope.md` as authoritative and ignore the fragment; 2. ask the product owner.
- Human response: none
- Resolution: option 1

## D-07: Timestamp correction for D-03, D-04, D-05
- Timestamp: 2026-10-19T12:36Z
- Phase: 1
- Type: non-blocking
- Trigger: D-03, D-04 and D-05 carry timestamps 12:34Z, 12:52Z and 12:55Z; the clock (`stat.mjs`, phase 0 end 12:36:02Z) shows all three were written between 12:33Z and 12:36Z. Records are not rewritten.
- Options: 1. (proposed default) read the three timestamps as 2026-10-19T12:35Z; take times from `date -u` from now on.
- Human response: none
- Resolution: option 1

## D-08: UI text in English
- Timestamp: 2026-10-19T12:41Z
- Phase: 2
- Type: non-blocking
- Trigger: the inputs do not state the UI language; the criteria and scenarios use English terms ("external participant", "student"), the conference is Slovenian.
- Options: 1. (proposed default) English UI text exactly as in `docs/02_contracts/ui.md`; 2. Slovenian UI text.
- Human response: none
- Resolution: pending review

## D-09: Behaviour the requirements leave open (status codes and limits)
- Timestamp: 2026-10-19T12:41Z
- Phase: 2
- Type: non-blocking
- Trigger: requirements check G-3 (AC-008-04 says "refused" without a status), G-4 (SR-03 gives no numbers), and no maximum field lengths in BR-01.
- Options: 1. (proposed default, the more conservative) 403 for organizer credentials over plain HTTP from a non-loopback client; rate limits per client 60/min form configuration, 10/min registration, 10/min export (failed logins included); request body limit 16 KiB (413); maximum lengths names 100, email 254, other text 200; unknown JSON properties rejected; fields of the other registration type ignored and not stored; all configurable (`docs/02_contracts/configuration.md`); 2. no limits.
- Human response: none
- Resolution: pending review

## D-10: Dev-only dependency @types/node for the end-to-end tests
- Timestamp: 2026-10-19T12:55Z
- Phase: 3
- Type: non-blocking
- Trigger: the end-to-end tests read the database row and the JSON copy inside the containers (`docker compose exec`, E2E-01, E2E-04) through `node:child_process`; TypeScript needs `@types/node`, which `tech-stack.md` does not list and nothing installs transitively.
- Options: 1. (proposed default) add `@types/node` 24.19.2 (MIT, matches Node 24) as an exact dev dependency of the frontend under the `tech-stack.md` rule for unlisted dev tooling; scanned in phase 6; 2. write the end-to-end tests in untyped JavaScript.
- Human response: none
- Resolution: option 1
