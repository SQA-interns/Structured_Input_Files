# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | pass — all lines filled; Run ID and Starting commit left empty and recorded by `stat.mjs init` (run id `kyuhi-single-agent-sdd-v005-opus-5-5-t1.5-20261012T1906Z-73c1`, commit `029038d`) |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | pass — see "Platforms and tooling"; one non-blocking difference (cloc, D-05) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | pass — Docker Engine 29.8.1 running (Linux engine); all pinned images pulled and started; Testcontainers starts `postgres:16.15-alpine` (probe test, `out/logs/00_testcontainers-probe.log`); Maven Central and npm registry reachable |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | pass — see "Secrets" |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | pass — `mvnw dependency:list` and `npm install` resolve every pin exactly (`out/logs/00_backend-deps.txt`, `out/logs/00_backend-deps-test.txt`, `frontend/package-lock.json`) |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | **blocked** — test-scope matches D-07, D-08 await the human; PA-01/PA-02/PA-03 applied (D-02, D-03, D-04) |
| Clean working tree on the starting commit | repository | pass — `git status` clean apart from untracked `03_statistics/run-log.json`; `.env` excluded via `.git/info/exclude`; HEAD `029038d` |
| Input manifest written | `docs/00_input-manifest.sha256` | pass — 38 files, LF-normalised SHA-256 |

## Platforms and tooling

Method: each tool's version command, re-run a second time for any mismatch.

| id | pinned | observed | Result |
|---|---|---|---|
| java | 21.0.10+7 | `java -version`: Temurin-21.0.10+7 | pass |
| node | 24.13.0 (npm 11.6.2) | `node --version` v24.13.0, `npm --version` 11.6.2 | pass |
| docker | 29.8.1 | `docker --version` 29.8.1, server 29.8.1 | pass |
| compose | 5.5.1 | `docker compose version` v5.5.1 | pass |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | `mvnw` script header 3.3.2; `mvnw -v` Apache Maven 3.9.9 | pass |
| spring-boot-maven-plugin | 4.1.1 | `mvnw package` | pass |
| spotless-maven-plugin | 2.44.3 | `spotless:check` | pass |
| spotbugs-maven-plugin | 4.10.4.1 | `spotbugs:check` | pass |
| maven-pmd-plugin | 3.26.0 | `pmd:check pmd:cpd-check` | pass |
| jacoco-maven-plugin | 0.8.12 | `jacoco:help` | pass |
| pitest-maven (+ junit5 plugin 1.2.3) | 1.30.0 | `pitest:help` | pass |
| dependency-check-maven | 12.1.0 | full scan completed (NVD analyser) | pass (D-01) |
| vite | 6.4.3 | `npm run build` | pass |
| typescript | 5.9.3 | `tsc --noEmit` | pass |
| eslint, @eslint/js, typescript-eslint, react-hooks, react-refresh | 9.39.5, 9.39.5, 8.70.1, 5.2.0, 0.4.26 | `eslint .` | pass |
| prettier | 3.9.9 | `prettier --check .` | pass |
| vitest, @vitest/coverage-v8 | 4.1.11 | `vitest --version` 4.1.11 | pass |
| @playwright/test | 1.63.0 | `playwright --version` 1.63.0; bundled Chromium v1243 present | pass |
| @stryker-mutator/core, vitest-runner | 10.0.0 | `stryker --version` 10.0.0 | pass |
| jscpd | 5.4.0 | `jscpd --version` 5.4.0 | pass |
| npm audit | 11.6.2 | `npm audit` ran | pass |
| semgrep/semgrep | 1.177.0 | `semgrep --version` 1.177.0 | pass |
| zricethezav/gitleaks | v8.30.1 | `gitleaks version` v8.30.1 | pass |
| aldanial/cloc | 2.10 | `cloc --version` 1.98 (image tag resolves and runs) | non-blocking, D-05 |
| images postgres, eclipse-temurin, node, nginx, mailpit | as pinned | `docker pull` and version command in each container: PostgreSQL 16.15, OpenJDK 21.0.10, Node v24.13.0, nginx 1.30.5, mailpit v1.31.1 | pass |

## Secrets

Method: `sed -n 's/^KEY=//p' .env | tr -d ' \r"'"'"'' | grep -q .` per key; values never displayed.

| Key | Marked | Result |
|---|---|---|
| POSTGRES_PASSWORD | provided | non-empty |
| ORGANIZER_USERNAME, ORGANIZER_PASSWORD | provided | non-empty |
| ORGANIZER_EMAILS | provided | non-empty |
| RECAPTCHA_SITE_KEY, RECAPTCHA_SECRET_KEY | test-mode only | empty — not required (local and tests use test mode) |
| SMTP_USERNAME, SMTP_PASSWORD | not needed | non-empty (unused locally) |
| NVD_API_KEY | provided | non-empty; NVD download succeeded |

## Dependency results

Scanners: OWASP Dependency-Check 12.1.0 (NVD analyser, all scopes incl. test, 112 dependencies, report `out/logs/00_dependency-check-report.json`); `npm audit` (npm 11.6.2, `out/logs/00_npm-audit.json`).

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| backend runtime (all Spring Boot starters 4.1.1, flyway 12.4.0, postgresql 42.7.13, poi-ooxml 5.5.1, tomcat-embed-core 11.0.26, log4j-api 2.26.1, commons-lang3 3.20.0) | as pinned | yes | none after PA-01 (angus-activation CVE-2025-7962, D-02) and PA-02 (hibernate-validator CVE-2025-15104, D-03) |
| spring-boot-starter-test, spring-boot-starter-webmvc-test | 4.1.1 | yes | none |
| testcontainers-junit-jupiter | 2.0.5 | yes | Critical 9.1 in transitive `docker-java-transport-zerodep` 3.7.1 (shaded httpclient5/httpcore5) — D-08 |
| testcontainers-postgresql | 2.0.5 | yes | Critical 9.8 by CPE name collision with PostgreSQL server — D-07 |
| archunit-junit5 | 1.3.2 | yes | none |
| frontend runtime (react, react-dom) | 19.3.0 | yes | none (`npm audit --omit=dev`: 0) |
| frontend dev tooling (all listed npm pins, @types/node 24.19.2) | as pinned | yes | Moderate `qs` via `@stryker-mutator/core` — PA-03, D-04 |
| container images | as pinned | yes | not scanned (no image scanner in `tooling`) |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-10-12T19:12Z | D-07, D-08: Critical/High Dependency-Check results on test-scope Testcontainers artifacts | pending | |
