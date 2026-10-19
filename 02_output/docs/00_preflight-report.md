# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `kyuhi-single-agent-tdd-v001-opus-5-5-t2.0-20261019T1224Z-583e`, starting commit `0fab2b2`. Logs: `out/logs/phase0-*`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | pass: every line filled; Run ID and Starting commit empty as required; `stat.mjs init` wrote them to `run-log.json` |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | pass: see "Platforms and tools"; one image reports another version (D-03, non-blocking) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | pass after D-01: Docker engine first unreachable, started by the human; every pinned image pulled and run; Maven Central, npm registry and Docker Hub reachable |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | pass: POSTGRES_PASSWORD, ORGANIZER_USERNAME, ORGANIZER_PASSWORD, ORGANIZER_EMAILS, NVD_API_KEY non-empty; RECAPTCHA_* test-mode only (empty), SMTP_* not needed (present) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | pass: every Maven POM returns HTTP 200 from Maven Central, every npm version from `npm view`, every image tag from the Docker Hub API and `docker pull`; `mvnw dependency:tree` and `npm install` resolve the exact versions |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | pass: Dependency-Check 12.1.0 (NVD analyser; OSS Index disabled, D-04) on the backend: 2 matches, both pre-approved false positives (D-05), 0 open after suppression with `failBuildOnCVSS=7`; `npm audit`: 2 Moderate in dev tooling only, pre-approved (D-02), `--omit=dev` 0 |
| Clean working tree on the starting commit | repository | pass: `git status` clean on `kyuhi/single-agent/tdd/v001` at `0fab2b2` before the first commit (`.env` untracked via `.git/info/exclude`) |
| Input manifest written | `docs/00_input-manifest.sha256` | pass: 41 files (`01_input/**`, `AGENTS.md`, `README.md`, `Run_Guide.md`, `03_statistics/metrics.md`, `run-log.template.json`, `usage.md`, `03_statistics/tools/**`), SHA-256 after CR removal |

## Platforms and tools

| id | pinned | method | observed |
|---|---|---|---|
| java | 21.0.10+7 | `java -version` | Temurin-21.0.10+7 |
| node / npm | 24.13.0 / 11.6.2 | `node --version`, `npm --version` | v24.13.0 / 11.6.2 |
| docker | 29.8.1 | `docker version` (client and server) | 29.8.1 / 29.8.1 (after D-01) |
| compose | 5.5.1 | `docker compose version` | v5.5.1 |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | `mvnw -v` | Maven 3.9.9 via wrapper 3.3.2 (only-script, SHA-1 of the distribution checked against Maven Central) |
| spring-boot-maven-plugin | 4.1.1 | `mvnw package` | builds `registration-backend-1.0.0.jar` |
| spotless-maven-plugin | 2.44.3 | `mvnw spotless:check` | runs, clean |
| spotbugs-maven-plugin | 4.10.4.1 | `mvnw spotbugs:check` | runs, 1 class analysed, 0 bugs |
| maven-pmd-plugin | 3.26.0 | `mvnw pmd:check pmd:cpd-check` | runs, clean |
| jacoco-maven-plugin | 0.8.12 | `mvnw jacoco:help` | resolves and runs |
| pitest-maven / pitest-junit5-plugin | 1.30.0 / 1.2.3 | POM resolution | resolves; first mutation run in phase 5 (needs tests) |
| dependency-check-maven | 12.1.0 | `mvnw dependency-check:check` | runs with NVD_API_KEY |
| vite, typescript, eslint, prettier, vitest, playwright, stryker, jscpd | as listed | `npx <tool> --version` | 6.4.3, 5.9.3, 9.39.5, 3.9.9, 4.1.11, 1.63.0, 10.0.0, 5.4.0; `npm run check` and `npm run build` pass |
| Playwright Chromium | bundled with 1.63.0 | `playwright install --dry-run chromium` | chromium-1243 already present in the user cache |
| npm audit | 11.6.2 | `npm audit` | runs (D-02) |
| semgrep/semgrep | 1.177.0 | `semgrep --version` in the image | 1.177.0 |
| zricethezav/gitleaks | v8.30.1 | `gitleaks version` in the image | v8.30.1 |
| aldanial/cloc | 2.10 | `cloc --version` in the image | 1.98 (D-03) |
| postgres, eclipse-temurin, node, nginx, mailpit images | as listed | version command in each image | 16.15, 21.0.10, v24.13.0, 1.30.5, v1.31.1 |

## Dependency results

Resolved versions from `out/logs/phase0-backend-dependency-tree.log` and `frontend/package-lock.json`.

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes (Maven Central) | none |
| spring-boot-starter-webmvc | 4.1.1 | yes (Maven Central) | none |
| spring-boot-starter-data-jpa | 4.1.1 | yes (Maven Central) | none |
| spring-boot-starter-validation | 4.1.1 (pulls hibernate-validator 9.1.3.Final) | yes (Maven Central) | Low, false positive (CVE-2025-15104, PA-02, D-05) |
| spring-boot-starter-mail | 4.1.1 (pulls angus-mail 2.0.5, angus-activation 2.0.3) | yes (Maven Central) | Low, false positive (CVE-2025-7962 on angus-activation, CVSS 7.5, PA-01, D-05) |
| spring-boot-starter-actuator | 4.1.1 | yes (Maven Central) | none |
| spring-boot-starter-security | 4.1.1 | yes (Maven Central) | none |
| spring-boot-starter-flyway | 4.1.1 | yes (Maven Central) | none |
| org.flywaydb:flyway-core | 12.4.0 | yes (Maven Central) | none |
| org.flywaydb:flyway-database-postgresql | 12.4.0 | yes (Maven Central) | none |
| org.postgresql:postgresql | 42.7.13 | yes (Maven Central) | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes (Maven Central) | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes (Maven Central) | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes (Maven Central) | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes (Maven Central) | none |
| spring-boot-starter-test | 4.1.1 (JUnit 6.0.3, Mockito 5.23.0) | yes (Maven Central) | none |
| spring-boot-starter-webmvc-test | 4.1.1 | yes (Maven Central) | none |
| org.testcontainers:testcontainers-junit-jupiter | 2.0.5 | yes (Maven Central) | none |
| org.testcontainers:testcontainers-postgresql | 2.0.5 | yes (Maven Central) | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes (Maven Central) | none |
| react | 19.3.0 | yes (npm) | none |
| react-dom | 19.3.0 | yes (npm) | none |
| @types/react | 19.3.0 | yes (npm) | none |
| @types/react-dom | 19.3.0 | yes (npm) | none |
| @vitejs/plugin-react | 4.7.0 | yes (npm) | none |
| @testing-library/react | 16.3.3 | yes (npm) | none |
| @testing-library/jest-dom | 6.9.1 | yes (npm) | none |
| jsdom | 26.1.0 | yes (npm) | none |
| globals | 15.15.0 | yes (npm) | none |
| npm dev tooling (vite, vitest, stryker, …) | as listed | yes (npm) | Medium, accepted: `qs` via `@stryker-mutator/core` (PA-03, D-02); `npm audit --omit=dev`: 0 |
| postgres | 16.15-alpine | yes (Docker Hub) | not scanned (image; phase 6) |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes (Docker Hub) | not scanned (image; phase 6) |
| node | 24.13.0-alpine | yes (Docker Hub) | not scanned (image; phase 6) |
| nginx | 1.30.5-alpine | yes (Docker Hub) | not scanned (image; phase 6) |
| axllent/mailpit | v1.31.1 | yes (Docker Hub) | not scanned (image; phase 6) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-19T12:31Z | Docker engine not running (D-01) | 12:32Z: Docker Desktop started | `docker version`: server 29.8.1; all images pulled and run |
