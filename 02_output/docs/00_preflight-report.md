# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261010T2210Z-7c3c`, starting commit `af81ef5`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required; `stat.mjs init` recorded both. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass. Java `java -version` → Temurin 21.0.10+7; Node `node --version` → v24.13.0, `npm --version` → 11.6.2; Compose `docker compose version` → v5.5.1; Docker Engine `docker --version` and `docker info` → 29.8.1, pinned 29.8.0, accepted by the human (D-01, D-07); Maven wrapper 3.3.2 → `./mvnw --version` Apache Maven 3.9.9; every Maven plugin resolved and ran (spotless:check, pmd:check, pmd:cpd-check, spotbugs:check, jacoco prepare-agent, pitest help, dependency-check:check); every npm tool printed its pinned version (`npx <tool> --version`); Playwright 1.63.0 Chromium (v1243) already present; containers: semgrep 1.177.0, gitleaks v8.30.1, cloc tag 2.10 prints 1.98 (D-02). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker daemon reachable (`docker info`); all runtime images pulled and started (`postgres --version` 16.15, `nginx -v` 1.30.5, `java -version` 21.0.10 in the Temurin JRE image, `node --version` v24.13.0, `mailpit version` v1.31.1). The local stack itself is written in phase 4. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. Checked with `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` and a second time by listing key names with masked values. Present: POSTGRES_PASSWORD, ORGANIZER_USERNAME, ORGANIZER_PASSWORD (length ≥ 16 checked without display), ORGANIZER_EMAILS, NVD_API_KEY. Empty and allowed: RECAPTCHA_SITE_KEY, RECAPTCHA_SECRET_KEY (test-mode only). SMTP_USERNAME and SMTP_PASSWORD (not needed) are set; unused locally. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Maven: `./mvnw dependency:list` shows every listed artifact at its exact version (`logs/phase0-backend-deplist.log`), Boot-managed Spring Framework 7.0.9, Security 7.1.1, JUnit 6.0.3, Mockito 5.23.0. npm: `npm ls --depth=0` shows every listed package at its exact version, lock file committed. Images: `docker pull` of every pinned tag (`logs/phase0-docker-pull.log`). |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass. Dependency-Check 12.1.0 (NVD analyser, D-03): CVE-2025-7962 on angus-activation 2.0.3 (CVSS 7.5, classified Low as a false positive by the human, D-04, D-07); CVE-2025-15104 on hibernate-validator (false positive, D-05); both suppressed per jar in `backend/dependency-check-suppressions.xml`, re-run: 0 open (`logs/phase0-backend-dependency-check-rerun.log`). `npm audit`: 2 moderate in dev-only Stryker dependencies (D-06), no Critical or High. |
| Clean working tree on the starting commit | repository | Pass. `git status` clean on `af81ef5` apart from the untracked `03_statistics/run-log.json`; `.env` ignored. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 35 files (`01_input/**`, `AGENTS.md`, `README.md`, `Run_Guide.md`, protected `03_statistics/` files), SHA-256 after CR removal. |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-data-jpa | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | yes | Medium, CVE-2025-15104 on transitive hibernate-validator 9.1.3 (false positive, D-05) |
| org.springframework.boot:spring-boot-starter-mail | 4.1.1 | yes | High by CVSS, CVE-2025-7962 on transitive angus-activation 2.0.3 (false positive, Low after D-07) |
| org.springframework.boot:spring-boot-starter-actuator | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-security | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-flyway | 4.1.1 | yes | none |
| org.flywaydb:flyway-core | 12.4.0 | yes | none |
| org.flywaydb:flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes | none |
| org.springframework.boot:spring-boot-starter-test | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-webmvc-test | 4.1.1 | yes | none |
| org.testcontainers:testcontainers-junit-jupiter | 2.0.5 | yes | none |
| org.testcontainers:testcontainers-postgresql | 2.0.5 | yes | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes | none |
| react | 19.3.0 | yes | none |
| react-dom | 19.3.0 | yes | none |
| @types/react | 19.3.0 | yes | none |
| @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react | 16.3.3 | yes | none |
| @testing-library/jest-dom | 6.9.1 | yes | none |
| jsdom | 26.1.0 | yes | none |
| globals | 15.15.0 | yes | none |
| postgres (image) | 16.15-alpine | yes | not scanned (image; runtime only) |
| eclipse-temurin (image) | 21.0.10_7-jre-alpine | yes | not scanned (image) |
| node (image) | 24.13.0-alpine | yes | not scanned (image) |
| nginx (image) | 1.30.5-alpine | yes | not scanned (image) |
| axllent/mailpit (image) | v1.31.1 | yes | not scanned (image) |
| npm tooling (vite … jscpd) | as listed | yes | Medium: qs 6.15.1 via @stryker-mutator/core (dev only, D-06) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-10 (phase 0) | D-01: accept Docker Engine 29.8.1 instead of 29.8.0; D-04: classify CVE-2025-7962 on angus-activation as a false positive | D-01: accept 29.8.1; D-04: false positive (D-07) | scan re-run with suppressions: 0 open; Docker 29.8.1 accepted |
