# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run: `kyuhi-single-agent-sdd-v004-opus-5-5-t1.4-20261011T0903Z-4af9`, starting commit `d2f7e14`. Raw output in `02_output/logs/phase0-*`.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required; `stat.mjs init` wrote them to `run-log.json`. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass. See "Platforms and tooling". One tag/version difference (cloc), non-blocking D-01. |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker Engine running; Docker Hub and Maven Central reachable (all pulls and resolutions succeeded); PostgreSQL and Mailpit images run; Playwright's bundled Chromium for 1.63.0 already present (`playwright install --dry-run`), nothing installed. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty (method: `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .`, values never displayed). `RECAPTCHA_*` test-mode only, `SMTP_*` not needed. `.env` ignored via `.git/info/exclude` and `.gitignore`. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. Maven: `dependency:list` (`phase0-backend-deps.txt`) and `dependency:resolve-plugins` (`phase0-backend-plugins.txt`); npm: `npm install` + `npm ls --depth=0`; images: `docker pull` (`phase0-docker-pull.log`). |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass after pre-approved items. Dependency-Check 12.1.0 (NVD, OSS Index disabled D-02): CVE-2025-7962 (7.5) and CVE-2025-15104 (5.3) suppressed per PA-01/PA-02 (D-03). `npm audit`: 2 Moderate, dev-only via Stryker, `--omit=dev` 0 (PA-03, D-04). |
| Clean working tree on the starting commit | repository | Pass. `git status` showed only the untracked `03_statistics/run-log.json` on `d2f7e14` (branch `kyuhi/single-agent/sdd/v004`). |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. 36 files (`01_input/**`, `AGENTS.md`, `README.md`, `Run_Guide.md`, `03_statistics/metrics.md`, `run-log.template.json`, `usage.md`, `03_statistics/tools/**`), LF-normalised SHA-256. |

## Platforms and tooling

| id | Pinned | Method | Observed |
|---|---|---|---|
| java | 21.0.10+7 | `java -version` | Temurin-21.0.10+7 |
| node | 24.13.0 (npm 11.6.2) | `node --version`, `npm --version` | v24.13.0, 11.6.2 |
| docker | 29.8.1 | `docker version` | server and client 29.8.1 |
| compose | 5.5.1 | `docker compose version` | v5.5.1 |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | wrapper scripts from Maven Central (SHA-1 verified), `./mvnw -v` | 3.3.2, Apache Maven 3.9.9 |
| spring-boot-maven-plugin | 4.1.1 | build log | 4.1.1 |
| spotless-maven-plugin | 2.44.3 | `./mvnw -Pcheck verify` | 2.44.3, runs |
| spotbugs-maven-plugin | 4.10.4.1 | `./mvnw -Pcheck verify` | 4.10.4.1, runs |
| maven-pmd-plugin | 3.26.0 | `./mvnw -Pcheck verify` (pmd, cpd) | 3.26.0, runs |
| jacoco-maven-plugin | 0.8.12 | `dependency:resolve-plugins` | 0.8.12 |
| pitest-maven (+ junit5 plugin) | 1.30.0 (1.2.3) | `dependency:resolve-plugins` | 1.30.0, 1.2.3 |
| dependency-check-maven | 12.1.0 | scan run | 12.1.0, BUILD SUCCESS |
| vite | 6.4.3 | `npx vite --version` | 6.4.3 |
| typescript | 5.9.3 | `npx tsc --version` | 5.9.3 |
| eslint, @eslint/js | 9.39.5 | `npx eslint --version`, `npm ls` | 9.39.5 |
| typescript-eslint | 8.70.1 | `npm ls` | 8.70.1 |
| eslint-plugin-react-hooks / -refresh | 5.2.0 / 0.4.26 | `npm ls` | 5.2.0 / 0.4.26 |
| prettier | 3.9.9 | `npx prettier --version` | 3.9.9 |
| vitest, @vitest/coverage-v8 | 4.1.11 | `npx vitest --version`, `npm ls` | 4.1.11 |
| @playwright/test | 1.63.0 | `npx playwright --version` | 1.63.0 |
| @stryker-mutator/core, vitest-runner | 10.0.0 | `npx stryker --version`, `npm ls` | 10.0.0 |
| jscpd | 5.4.0 | `npx jscpd --version` | 5.4.0 |
| npm audit | 11.6.2 | `npm --version` | 11.6.2 |
| semgrep/semgrep | 1.177.0 | `semgrep --version` in the image | 1.177.0 |
| zricethezav/gitleaks | v8.30.1 | `gitleaks version` | v8.30.1 |
| aldanial/cloc | 2.10 | `cloc --version` in the image | 1.98 (D-01, non-blocking) |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| spring-boot-starter-parent | 4.1.1 | yes | none |
| spring-boot-starter-webmvc / data-jpa / validation / mail / actuator / security / flyway | 4.1.1 | yes | none reported on the starters; transitive: hibernate-validator 9.1.3.Final CVE-2025-15104 Medium → Low (PA-02), angus-activation 2.0.3 CVE-2025-7962 High → Low (PA-01) |
| flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| postgresql | 42.7.13 | yes | none |
| poi-ooxml | 5.5.1 | yes | none |
| tomcat-embed-core | 11.0.26 | yes | none |
| log4j-api | 2.26.1 | yes | none |
| commons-lang3 | 3.20.0 | yes | none |
| spring-boot-starter-test, -webmvc-test | 4.1.1 | yes (JUnit Jupiter 6.0.3, Mockito 5.23.0) | none |
| testcontainers-junit-jupiter, -postgresql | 2.0.5 | yes | none |
| archunit-junit5 | 1.3.2 | yes | none |
| react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react | 16.3.3 | yes | none |
| @testing-library/jest-dom | 6.9.1 | yes | none |
| jsdom | 26.1.0 | yes | none |
| globals | 15.15.0 | yes | none |
| @stryker-mutator/core (tooling) | 10.0.0 | yes | Moderate via qs (dev only, PA-03) |
| postgres | 16.15-alpine | yes | not scanned (no container scanner in tooling) |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes | not scanned |
| node | 24.13.0-alpine | yes | not scanned |
| nginx | 1.30.5-alpine | yes | not scanned |
| axllent/mailpit | v1.31.1 | yes | not scanned |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| — | nothing | — | — |
