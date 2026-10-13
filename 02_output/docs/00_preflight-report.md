# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run `tanej-single-agent-sdd-v001-opus-5-5-t1.7-20261013T1947Z-3bcb`, started 2026-10-13T19:47Z on commit `425cab9`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required; `stat.mjs init` and `phase 0 start` recorded them. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass with approved deviations. Node 24.13.0 and npm 11.6.2 match. JDK: Oracle 21.0.11 instead of Temurin 21.0.10+7 (D-01, PA-05). Docker Engine 29.8.2 instead of 29.8.1 (D-02, PA-05). Docker Compose 5.6.0 instead of 5.5.1: minor version, not covered by PA-05 (D-03, approved by the human). All tooling resolves and runs at the pinned version, except the `aldanial/cloc:2.10` image, which reports 1.98 (D-04). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Desktop running; Docker Hub, Maven Central and the npm registry reachable; `postgres`, `axllent/mailpit`, `eclipse-temurin`, `node`, `nginx` images pulled and started at the pinned tags. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty. `RECAPTCHA_*` (test-mode only) and `SMTP_*` (not needed) are empty, as allowed. `.env` is git-ignored. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Maven dependencies, plugins and Boot overrides resolve at the pinned versions (`dependency:tree`, `dependency:resolve-plugins`, `<plugin>:help`); `npm ls --depth=0` shows every npm pin; every container image pulls. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass after pre-approved items. Dependency-Check 12.1.0 (NVD with API key, OSS Index disabled, D-05): two matches, both pre-approved false positives (D-06 PA-01, D-07 PA-02), re-run clean. `npm audit`: 2 Moderate (`qs` via `@stryker-mutator/core`), `npm audit --omit=dev` clean (D-08, PA-03). |
| Clean working tree on the starting commit | repository | Pass. `git status` clean on `425cab9` before `stat.mjs init`; only `03_statistics/run-log.json` and the git-ignored `.env` were untracked afterwards. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 41 files: everything under `01_input/`, `AGENTS.md`, `README.md`, `Run_Guide.md`, `.claude/settings.json`, `03_statistics/metrics.md`, `run-log.template.json`, `usage.md`, `tools/*`; SHA-256 after CR removal. |

## Methods

| Check | First method | Second method (failures only) |
|---|---|---|
| JDK | `java -version` | `javac -version`; `java -XshowSettings:properties` (vendor, version) |
| Docker Engine | `docker version` | `docker info` (server version) |
| Docker Compose | `docker compose version` | `docker compose version --short`; `docker info` client plugin list |
| Node, npm | `node --version`, `npm --version` | |
| Container tools | `docker run --rm <image> <version command>` | cloc: tag `2.10` pulled by digest, `--version` prints 1.98 |
| npm tooling | `npx <tool> --version`, `npm ls --depth=0` | |
| Playwright Chromium | `npx playwright install --dry-run chromium` | build v1243 present in the local Playwright cache |
| Secrets | `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .` per key, values never displayed | |
| Leak check of the logs | `grep -lF -f <(.env values ≥ 6 chars)` over `02_output/logs/*` | no file listed |

## Bootstrap

| Component | Build file | Skeleton | Build | Check |
|---|---|---|---|---|
| backend | `backend/pom.xml`, Maven Wrapper 3.3.2 (Maven 3.9.9) | `si.konferenca.registration.RegistrationApplication` | `./mvnw -B package -DskipTests`: success | `./mvnw -B spotless:check compile pmd:check pmd:cpd-check spotbugs:check`: success |
| frontend | `frontend/package.json`, `package-lock.json` | Vite + React + TypeScript app shell | `npm run build`: success | `npm run check` (Prettier, ESLint, `tsc --noEmit`): success; `npm test` runs (no tests yet) |

Logs: `out/logs/00_*.log`.

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes | none |
| spring-boot-starter-webmvc, -data-jpa, -validation, -mail, -actuator, -security, -flyway | 4.1.1 | yes | hibernate-validator 9.1.3 (transitive): CVE-2025-15104, Low, false positive (PA-02) |
| org.flywaydb:flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| org.postgresql:postgresql | 42.7.13 | yes | none |
| org.apache.poi:poi-ooxml | 5.5.1 | yes | none |
| org.apache.tomcat.embed:tomcat-embed-core | 11.0.26 | yes (override applied) | none |
| org.apache.logging.log4j:log4j-api | 2.26.1 | yes (override applied) | none |
| org.apache.commons:commons-lang3 | 3.20.0 | yes (override applied) | none |
| spring-boot-starter-mail → angus-activation 2.0.3 / angus-mail 2.0.5 | (transitive) | yes | CVE-2025-7962, Low, false positive (PA-01) |
| spring-boot-starter-test, -webmvc-test | 4.1.1 | yes (JUnit 6.0.3, Mockito 5.23.0) | none |
| org.testcontainers:testcontainers-junit-jupiter, testcontainers-postgresql | 2.0.5 | yes | none |
| com.tngtech.archunit:archunit-junit5 | 1.3.2 | yes | none |
| react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react / jest-dom | 16.3.3 / 6.9.1 | yes | none |
| jsdom / globals | 26.1.0 / 15.15.0 | yes | none |
| npm tooling (vite 6.4.3, typescript 5.9.3, eslint 9.39.5, typescript-eslint 8.70.1, prettier 3.9.9, vitest 4.1.11, playwright 1.63.0, stryker 10.0.0, jscpd 5.4.0, …) | as listed | yes | `qs` via `@stryker-mutator/core`: Moderate → Medium, accepted (PA-03) |
| Maven tooling (spotless 2.44.3, spotbugs 4.10.4.1, pmd 3.26.0, jacoco 0.8.12, pitest 1.30.0 + junit5 plugin 1.2.3, dependency-check 12.1.0) | as listed | yes | not scanned (build tooling) |
| Images: postgres 16.15-alpine, eclipse-temurin 21.0.10_7-jre-alpine, node 24.13.0-alpine, nginx 1.30.5-alpine, mailpit v1.31.1, semgrep 1.177.0, gitleaks v8.30.1, cloc 2.10 | as listed | yes | not scanned in phase 0 (no image scanner listed) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-13 phase 0 | Docker Compose 5.5.1 (installed 5.6.0): approve 5.6.0 or install 5.5.1 (D-03) | Approve 5.6.0 (2026-10-13T20:06:32Z) | `docker compose version`: v5.6.0, accepted |
