# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass. All lines filled; Run ID and Starting commit empty as required. `stat.mjs init` recorded run id `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261012T1355Z-5267` and commit `e74fb75`. Method: read the file; `stat.mjs` output. |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Fail, waiting on D-01** for JDK, Docker Engine, Compose (see table below). All other platforms and every tool pass; `cloc` reports another version (D-03, non-blocking). |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass. Docker daemon running (`docker info`); every container image in `tech-stack.md` resolves (`docker manifest inspect`); Maven Central and the npm registry answer HTTP 200. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass. Required keys `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty; `RECAPTCHA_*` test-mode only (empty); `SMTP_*` not needed. Method 1: the `sed … \| grep -q .` check from the skill; method 2: key names listed with values masked. No value displayed. |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass. 61 of 61 (Maven POM HTTP 200 on Maven Central, `npm view <id>@<version>`, `docker manifest inspect`). Resolved backend versions also confirmed with `mvnw dependency:list`. |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Fail, waiting on D-04 and D-05.** Frontend: 2 Critical, 5 High in dev tooling (`vitest`, `jscpd`). Backend: CVE-2025-7962 (7.5) on `angus-activation`, analysed as a false positive. Mediums recorded in D-06. |
| Clean working tree on the starting commit | repository | Pass. On `e74fb75`, nothing tracked modified; untracked `.env` only. Note: `.env` was not yet git-ignored at the start (Run guide step 1.2 not applied); the ES-03 `.gitignore` commit `c8172ac` now ignores it, and `.env` was never staged. |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass. Every file under `01_input/` plus the protected root and `03_statistics/` files of README sections 1 and 2, LF-normalised, `sha256sum` format. |

## Platforms and tools

Method 1: the tool's own version command. Method 2 (for every mismatch): a different query (`update-alternatives`, `ls /usr/lib/jvm`, `docker info`, `docker compose version --short`).

| id | Pinned | Reported | Result |
|---|---|---|---|
| java | Eclipse Temurin 21.0.10+7 | OpenJDK 21.0.12.1 (Ubuntu build); no Temurin installed | mismatch, D-01 |
| node | 24.13.0 (npm 11.6.2) | v24.13.0, npm 11.6.2 (nvm install; not on the default non-interactive `PATH`, used via `~/.nvm/versions/node/v24.13.0/bin`) | pass |
| docker | 29.8.0 | Engine 29.3.1 (Docker Desktop 4.67.0), CLI 29.5.0 | mismatch, D-01 |
| compose | 5.5.1 | 5.1.1 | mismatch, D-01 |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | wrapper 3.3.2 scripts (SHA-1 verified), Maven 3.9.9 (SHA-512 verified, SHA-256 pinned in `maven-wrapper.properties`) | pass |
| spring-boot-maven-plugin | 4.1.1 | 4.1.1, `package` succeeds | pass |
| spotless-maven-plugin | 2.44.3 | 2.44.3, `spotless:check` clean | pass |
| spotbugs-maven-plugin | 4.10.4.1 | 4.10.4.1, `spotbugs:check` clean | pass |
| maven-pmd-plugin | 3.26.0 | 3.26.0, `pmd:check` and `pmd:cpd-check` clean | pass |
| jacoco-maven-plugin | 0.8.12 | 0.8.12, agent and report run in `verify` | pass |
| pitest-maven (+ junit5 plugin 1.2.3) | 1.30.0 | 1.30.0 starts; "No mutations found" because the skeleton holds only the excluded entry point | pass |
| dependency-check-maven | 12.1.0 | 12.1.0, NVD analyser completes (OSS Index disabled, D-02) | pass |
| vite | 6.4.3 | 6.4.3 | pass |
| typescript | 5.9.3 | 5.9.3 | pass |
| eslint, @eslint/js | 9.39.5 | 9.39.5 | pass |
| typescript-eslint | 8.70.1 | 8.70.1 (`npm ls`) | pass |
| eslint-plugin-react-hooks | 5.2.0 | 5.2.0 (`npm ls`) | pass |
| eslint-plugin-react-refresh | 0.4.26 | 0.4.26 (`npm ls`) | pass |
| prettier | 3.9.9 | 3.9.9 | pass |
| vitest, @vitest/coverage-v8 | 3.2.7 | 3.2.7 (runs; vulnerable, D-04) | pass (version) |
| @playwright/test | 1.63.0 | 1.63.0 | pass |
| @stryker-mutator/core, vitest-runner | 10.0.0 | 10.0.0 | pass |
| jscpd | 4.3.0 | 4.3.0 (runs; vulnerable, D-04) | pass (version) |
| npm audit | npm 11.6.2 | 11.6.2 | pass |
| semgrep/semgrep | 1.177.0 | 1.177.0 | pass |
| zricethezav/gitleaks | v8.30.1 | v8.30.1 | pass |
| aldanial/cloc | 2.10 | 1.98 | runs, other version reported, D-03 |

## Component skeletons

| Component | Build | Check | Test | Log |
|---|---|---|---|---|
| backend | `./mvnw -B package -DskipTests` pass | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` pass | `./mvnw -B verify` pass (no tests yet) | `out/logs/phase0-backend-*.log` |
| frontend | `npm run build` pass | `npm run check` pass | `npm test` pass (no tests yet) | `out/logs/phase0-frontend-*.log` |

## Dependency results

Vulnerability tools: OWASP Dependency-Check 12.1.0 (NVD) for the backend's resolved set of 66 jars; `npm audit` (npm 11.6.2) for the frontend lock file. Container images resolve but are not vulnerability-scanned: `tooling` lists no image scanner. Levels per `general/quality/severity-scale.md` (CVSS mapping).

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| org.springframework.boot:spring-boot-starter-parent | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-webmvc | 4.1.1 | yes | none (incl. tomcat-embed-core 11.0.26) |
| org.springframework.boot:spring-boot-starter-data-jpa | 4.1.1 | yes | none |
| org.springframework.boot:spring-boot-starter-validation | 4.1.1 | yes | Medium: CVE-2025-15104 (5.3) on transitive hibernate-validator 9.1.3.Final (D-06) |
| org.springframework.boot:spring-boot-starter-mail | 4.1.1 | yes | High as reported: CVE-2025-7962 (7.5) on transitive angus-activation 2.0.3, false positive (D-05) |
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
| react, react-dom | 19.3.0 | yes | none |
| @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react | 16.3.3 | yes | none |
| @testing-library/jest-dom | 6.9.1 | yes | none |
| jsdom | 26.1.0 | yes | none |
| globals | 15.15.0 | yes | none |
| vitest, @vitest/coverage-v8 (tooling) | 3.2.7 | yes | Critical: tinypool prototype pollution to RCE (D-04) |
| jscpd (tooling) | 4.3.0 | yes | High: braces stack-exhaustion DoS (D-04) |
| @stryker-mutator/core (tooling) | 10.0.0 | yes | Medium: qs DoS via typed-rest-client (D-06) |
| other npm tooling (vite, typescript, eslint, prettier, playwright, …) | as pinned | yes | none |
| postgres | 16.15-alpine | yes | not scanned: `tooling` lists no container image scanner |
| eclipse-temurin | 21.0.10_7-jre-alpine | yes | not scanned: `tooling` lists no container image scanner |
| node | 24.13.0-alpine | yes | not scanned: `tooling` lists no container image scanner |
| nginx | 1.30.5-alpine | yes | not scanned: `tooling` lists no container image scanner |
| axllent/mailpit | v1.31.1 | yes | not scanned: `tooling` lists no container image scanner |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-12 | D-01 host JDK, Docker Engine, Compose versions; D-04 vulnerable vitest/jscpd; D-05 CVE-2025-7962 false positive | pending | pending |
