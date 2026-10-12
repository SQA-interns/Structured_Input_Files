# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK, Docker Engine and Compose differ from the pins
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platforms pin Eclipse Temurin JDK 21.0.10+7, Docker Engine 29.8.0 and Docker Compose 5.5.1. The host has OpenJDK 21.0.12.1 (Ubuntu build 21.0.12.1+1-1-24.04.4; no Temurin JDK installed), Docker Engine 29.3.1 (Docker Desktop 4.67.0; CLI 29.5.0) and Docker Compose 5.1.1. Confirmed twice: `java -version` and `update-alternatives --list java` / `ls /usr/lib/jvm`; `docker version` and `docker info`; `docker compose version` and `--short`. Node 24.13.0 and npm 11.6.2 match (from nvm). The backend skeleton builds and its check tools run on the installed JDK.
- Options: 1. Approve the installed versions for host tooling; container images and the Java release target (21) stay as pinned (proposed default). 2. The human installs Temurin 21.0.10+7, Docker Engine 29.8.0 and Compose 5.5.1, and the agent re-checks.
- Human response: none
- Resolution: pending

## D-02: OWASP Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: non-blocking
- Trigger: the Sonatype OSS Index analyser failed for every jar (it needs credentials that `secrets.env.example` does not list). Preflight step 6.
- Options: 1. Disable only that analyser (`ossindexAnalyzerEnabled=false` in `pom.xml`) and keep the NVD analyser, which uses `NVD_API_KEY` (chosen). 2. Ask for OSS Index credentials.
- Human response: none
- Resolution: option 1; re-run completed with no analyser errors (66 dependencies analysed)

## D-03: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: non-blocking
- Trigger: `aldanial/cloc:2.10` resolves and runs, but `cloc --version` prints 1.98.
- Options: 1. Keep the pinned image tag as authoritative and record the reported version with code metrics (chosen, per `tech-stack.md` rules).
- Human response: none
- Resolution: option 1

## D-04: vitest, @vitest/coverage-v8 and jscpd have Critical/High vulnerabilities
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: blocking
- Trigger: `npm audit` on the pinned frontend set: 2 Critical, 5 High, 4 Moderate. Critical: `tinypool` <2.1.2 prototype pollution to RCE (GHSA 1241260, 1241261) via `vitest` 3.2.7. High: `braces` <=3.0.3 stack-exhaustion DoS (1240992) via `micromatch`, `fast-glob`, `@jscpd/finder` from `jscpd` 4.3.0. Moderate: `@vitest/mocker` path traversal (1193684); `qs` via `typed-rest-client` from `@stryker-mutator/core` 10.0.0. All are dev/test tooling; `npm audit --omit=dev` finds 0. Log: `out/logs/phase0-npm-audit.json`.
- Options: 1. Replace `vitest` and `@vitest/coverage-v8` 3.2.7 with 5.0.3, and `jscpd` 4.3.0 with 5.4.0 (proposed default: lowest versions that clear every Critical and High; trial install leaves 0 Critical, 0 High, 2 Moderate (`qs`, `typed-rest-client` under Stryker); test, check, coverage and jscpd run; vitest 5.0.3 accepts vite 6.4.x and Stryker's vitest-runner 10.0.0 accepts vitest >=2; all MIT). 2. Use the latest 5.0.4 and 5.4.1 (same audit result). 3. Keep 3.2.7 and 4.3.0 and accept the risk.
- Human response: none
- Resolution: pending

## D-05: CVE-2025-7962 (High) reported on angus-activation 2.0.3
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: blocking
- Trigger: OWASP Dependency-Check (NVD) matched CVE-2025-7962, CVSS v3 7.5 (High), to `org.eclipse.angus:angus-activation:2.0.3` (pulled in by `spring-boot-starter-mail`). The CVE is an SMTP injection in Jakarta Mail; its CPEs are `angus_mail` before 2.0.4 and `jakarta_mail` before 1.6.8 / 2.0.2. The scanner mapped `angus-activation` to the `angus_mail` CPE by vendor/product name; the mail library actually on the classpath is `angus-mail` 2.0.5, which is not affected and which the scanner did not flag. Log: `out/logs/phase0-dependency-check-rerun.log`.
- Options: 1. Treat it as a false positive, lower it to Low, and suppress CVE-2025-7962 for `angus-activation` only in a Dependency-Check suppression file (proposed default). 2. Keep it High and block.
- Human response: none
- Resolution: pending

## D-06: Medium dependency findings recorded, not blocking
- Timestamp: 2026-10-12T14:03:39Z
- Phase: 0
- Type: non-blocking
- Trigger: CVE-2025-15104 (CVSS 5.3, Medium) on `hibernate-validator` 9.1.3.Final (Boot-managed via `spring-boot-starter-validation`); Moderate `qs`/`typed-rest-client` under `@stryker-mutator/core` 10.0.0 (dev only).
- Options: 1. Record as Medium, do not override versions not listed in `tech-stack.md`, re-scan in phase 6 (chosen). 2. Add version overrides (would add unlisted versions).
- Human response: none
- Resolution: option 1; pending review

## D-07: Human approves D-01 (installed host versions)
- Timestamp: 2026-10-12T15:43:48Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-01.
- Options: as in D-01.
- Human response: "i agree with d01, d04 and d05" (2026-10-12T15:43:48Z)
- Resolution: D-01 option 1. Host tooling: OpenJDK 21.0.12.1, Docker Engine 29.3.1, Docker Compose 5.1.1. Container images and the Java release target stay as pinned.

## D-08: Human approves D-04 (fixed vitest and jscpd versions)
- Timestamp: 2026-10-12T15:43:48Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-04.
- Options: as in D-04.
- Human response: "i agree with d01, d04 and d05" (2026-10-12T15:43:48Z)
- Resolution: D-04 option 1. `vitest` and `@vitest/coverage-v8` 5.0.3, `jscpd` 5.4.0 replace the `tech-stack.md` entries 3.2.7 and 4.3.0.

## D-09: Human approves D-05 (CVE-2025-7962 false positive)
- Timestamp: 2026-10-12T15:43:48Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-05.
- Options: as in D-05.
- Human response: "i agree with d01, d04 and d05" (2026-10-12T15:43:48Z)
- Resolution: D-05 option 1. Lowered to Low; suppressed for `org.eclipse.angus:angus-activation` only in `backend/dependency-check-suppressions.xml`.
