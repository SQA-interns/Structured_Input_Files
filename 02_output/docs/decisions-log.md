# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints `1.98` (image created 2023-08-19); tech-stack.md pins tag `2.10`. The pin resolves and the tool runs.
- Options: 1. keep the pinned tag `aldanial/cloc:2.10` as authoritative and use it unchanged in every measurement (default); 2. ask for another tag.
- Human response: none
- Resolution: option 1 (tech-stack.md rule: a pin that resolves and runs but reports another version is non-blocking)

## D-02: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: first run of `dependency-check-maven` 12.1.0 (`02_output/logs/phase0-dependency-check.log`) ended with errors from the Sonatype OSS Index Analyzer, which needs credentials that `secrets.env.example` does not list.
- Options: 1. disable `ossindexAnalyzerEnabled` and scan with the NVD-based analysers (default); 2. ask for OSS Index credentials.
- Human response: none
- Resolution: option 1 (preflight step 6); re-run completed (`phase0-dependency-check-rerun.log`, BUILD SUCCESS)

## D-03: CVE-2025-7962 and CVE-2025-15104 suppressed (PA-01, PA-02)
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High by severity-scale.md) on `angus-activation-2.0.3.jar` and CVE-2025-15104 (CVSS 5.3) on `hibernate-validator-9.1.3.Final.jar`. Evidence: resolved `angus-mail` is 2.0.5 (`phase0-backend-deps.txt`); CVE-2025-15104 is the Nu Html Checker CPE collision.
- Options: 1. apply pre-approved.md PA-01 and PA-02: classify Low, false positive, suppress for those artifacts only (default).
- Human response: none (pre-approved in `01_project/00_setup/pre-approved.md`)
- Resolution: option 1; suppressions in `backend/dependency-check-suppressions.xml`

## D-04: npm audit Moderate advisories via @stryker-mutator/core (PA-03)
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 Moderate (`qs` 6.15.1 via `typed-rest-client` 2.3.1 via `@stryker-mutator/core` 10.0.0); `npm audit --omit=dev` reports 0.
- Options: 1. apply pre-approved.md PA-03: accept, record as Medium, accepted (default).
- Human response: none (pre-approved)
- Resolution: option 1
