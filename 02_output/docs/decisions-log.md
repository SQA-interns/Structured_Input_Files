# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Host JDK is Oracle 21.0.11 instead of Temurin 21.0.10+7
- Timestamp: 2026-10-13T19:55Z
- Phase: 0
- Type: non-blocking
- Trigger: `tech-stack.md` platform `java` pins Eclipse Temurin JDK 21.0.10+7; `java -version` and `javac -version` report 21.0.11 (vendor Oracle Corporation). PA-05 in `pre-approved.md` covers a different patch version of the JDK; the vendor differs too. The runtime image stays `eclipse-temurin:21.0.10_7-jre-alpine` (verified: Temurin 21.0.10+7).
- Options: 1. Apply PA-05: use the installed 21.0.11 for host builds and tests, keep the pinned Temurin image for runtime (default); 2. install Temurin 21.0.10+7 on the host.
- Human response: none
- Resolution: 1 (PA-05); vendor difference pending review

## D-02: Docker Engine 29.8.2 instead of 29.8.1
- Timestamp: 2026-10-13T19:55Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker version` and `docker info` report 29.8.2; `tech-stack.md` pins 29.8.1. Patch release of the pinned minor; all pinned images pull and run. Matches PA-05.
- Options: 1. Approve 29.8.2 for this run under PA-05, do not edit `tech-stack.md` (default).
- Human response: none
- Resolution: 1 (PA-05)

## D-03: Docker Compose 5.6.0 instead of 5.5.1
- Timestamp: 2026-10-13T19:56Z
- Phase: 0
- Type: blocking
- Trigger: `docker compose version` and the Docker client plugin list report v5.6.0; `tech-stack.md` pins 5.5.1. This is a different minor version and Compose is not named in PA-05, so it is not pre-approved.
- Options: 1. Approve Compose 5.6.0 for this run (proposed default: the compose file format is unchanged and the local stack is only used for phases 3, 6 and 7); 2. the human installs Docker Compose 5.5.1.
- Human response: Approve Compose 5.6.0 for this run (2026-10-13T20:06:32Z)
- Resolution: 1 (approved by the human); `tech-stack.md` unchanged

## D-04: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-13T19:52Z
- Phase: 0
- Type: non-blocking
- Trigger: `aldanial/cloc:2.10` pulls and runs; `cloc --version` prints 1.98. A pin that resolves and runs but reports another version (`tech-stack.md` rules).
- Options: 1. Keep the pinned tag `2.10` as authoritative and record the reported version (default).
- Human response: none
- Resolution: 1

## D-05: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-13T19:58Z
- Phase: 0
- Type: non-blocking
- Trigger: the Sonatype OSS Index analyser of `dependency-check-maven` 12.1.0 fails on every artifact because it needs credentials that `secrets.env.example` does not list (preflight step 6).
- Options: 1. Set `ossindexAnalyzerEnabled=false` in `backend/pom.xml` and scan with the NVD (API key), RetireJS and remaining analysers (default).
- Human response: none
- Resolution: 1; re-run completed with NVD data (`logs/00_backend-dependency-check-rerun.log`)

## D-06: CVE-2025-7962 on angus-activation (PA-01)
- Timestamp: 2026-10-13T19:58Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5) on `org.eclipse.angus:angus-activation` 2.0.3. The shipped `angus-mail` is 2.0.5 (`dependency:tree`), matching PA-01.
- Options: 1. Classify Low, false positive; suppress for that one artifact only (default, PA-01).
- Human response: none
- Resolution: 1; suppression in `backend/dependency-check-suppressions.xml`

## D-07: CVE-2025-15104 on hibernate-validator (PA-02)
- Timestamp: 2026-10-13T19:58Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-15104 on `hibernate-validator` 9.1.3.Final through the CPE `validator:validator` (Nu Html Checker), matching PA-02.
- Options: 1. Classify Low, false positive; suppress for that one artifact only (default, PA-02).
- Human response: none
- Resolution: 1; suppression in `backend/dependency-check-suppressions.xml`

## D-08: npm audit Moderate advisories on qs (PA-03)
- Timestamp: 2026-10-13T19:59Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 Moderate advisories (`qs` 6.15.1, `typed-rest-client`), reached only through `@stryker-mutator/core` (`npm ls qs`); `npm audit --omit=dev` reports 0. Matches PA-03.
- Options: 1. Accept; record as Medium, accepted (default, PA-03).
- Human response: none
- Resolution: 1
