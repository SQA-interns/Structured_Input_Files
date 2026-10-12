# Preflight report

> Written in: phase 0 · Source: `project/00_setup/*` · Procedure: `general/skills/preflight` · Agent: writes

Run id `kyuhi-single-agent-sdd-v006-opus-5-5-t1.6-20261012T2325Z-0b56`, starting commit `6bf4a55`, checked 2026-10-12/13 (UTC).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Pass: model, effort, template version 1.6, label, operator, machine, plan, permission mode filled; Run ID and Starting commit empty and recorded by `stat.mjs init` (read the file, then ran `stat.mjs init`) |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | Pass: see "Platforms and tools"; one non-blocking deviation in reported version (D-01) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Pass: Docker Engine reachable (`docker info`: linux/x86_64); every pinned image pulled from Docker Hub and started once (`out/logs/00_image-pull.log`); Maven Central and the npm registry reachable (resolution below). Testcontainers and Mailpit run on this engine. |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Pass: `POSTGRES_PASSWORD`, `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS`, `NVD_API_KEY` non-empty (method: `sed -n 's/^KEY=//p' .env \| tr -d ' \r"'"'"'' \| grep -q .`, values never shown). `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` empty: test-mode only. `SMTP_USERNAME`, `SMTP_PASSWORD`: not needed (set anyway). `.env` is ignored (`git check-ignore`). |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Pass: Maven `dependency:resolve dependency:resolve-plugins` exit 0 (`out/logs/00_backend-resolve.log`, tree in `out/logs/00_backend-tree.log`); `npm view <pkg>@<version>` for every npm pin and `npm install` exit 0 (`out/logs/00_frontend-install.log`); images pulled |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Pass after pre-approved items: Dependency-Check 12.1.0 (NVD; OSS Index disabled, D-02) found CVE-2025-7962 (High, PA-01 → Low, D-03) and CVE-2025-15104 (Medium, PA-02 → Low, D-04); `npm audit`: 2 moderate on `qs` via Stryker only, `--omit=dev` clean (PA-03 → Medium accepted, D-05) |
| Clean working tree on the starting commit | repository | Pass: `git status` clean on `6bf4a55` before bootstrap, apart from the ignored `.env` |
| Input manifest written | `docs/00_input-manifest.sha256` | Pass: 39 files (all of `01_input/`, `AGENTS.md`, `README.md`, `Run_Guide.md`, `03_statistics/metrics.md`, `run-log.template.json`, `03_statistics/tools/*`), SHA-256 after CR removal |

## Platforms and tools

| id | Pinned | Command | Reported |
|---|---|---|---|
| java | 21.0.10+7 | `java -version` | Temurin-21.0.10+7 |
| node / npm | 24.13.0 / 11.6.2 | `node --version`, `npm --version` | v24.13.0 / 11.6.2 |
| docker | 29.8.1 | `docker version` | 29.8.1 (server and client) |
| compose | 5.5.1 | `docker compose version` | v5.5.1 |
| maven-wrapper | 3.3.2 (Maven 3.9.9) | `./mvnw -v` | Apache Maven 3.9.9, wrapperVersion 3.3.2 |
| spring-boot-maven-plugin, spotless, spotbugs, pmd, jacoco, pitest, dependency-check | as pinned | `./mvnw package spotless:check pmd:check pmd:cpd-check spotbugs:check`, `jacoco:help`, `pitest:help`, `dependency-check:check` | all ran at the pinned versions (PMD 7.7.0 engine) (`out/logs/00_backend-check.log`) |
| vite, typescript, eslint, prettier, vitest, playwright, stryker, jscpd | as pinned | `npx <tool> --version` | 6.4.3, 5.9.3, 9.39.5, 3.9.9, 4.1.11, 1.63.0 (Chromium v1243 present), 10.0.0, 5.4.0 |
| semgrep/semgrep | 1.177.0 | `semgrep --version` in the image | 1.177.0 |
| zricethezav/gitleaks | v8.30.1 | `gitleaks version` | v8.30.1 |
| aldanial/cloc | 2.10 | `cloc --version` | 1.98 (non-blocking, D-01) |
| postgres, eclipse-temurin, node, nginx, axllent/mailpit images | as pinned | version command in each image | 16.15, 21.0.10, v24.13.0, 1.30.5, v1.31.1 |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| spring-boot-starter-parent and starters (webmvc, data-jpa, validation, mail, actuator, security, flyway, test, webmvc-test) | 4.1.1 | yes | Low after PA-01/PA-02 (transitive `angus-activation` 2.0.3, `hibernate-validator` 9.1.3.Final; D-03, D-04) |
| flyway-core, flyway-database-postgresql | 12.4.0 | yes | none |
| postgresql | 42.7.13 | yes | none |
| poi-ooxml | 5.5.1 | yes | none |
| tomcat-embed-core | 11.0.26 | yes | none |
| log4j-api | 2.26.1 | yes | none |
| commons-lang3 | 3.20.0 | yes | none |
| testcontainers-junit-jupiter, testcontainers-postgresql | 2.0.5 | yes | none |
| archunit-junit5 | 1.3.2 | yes | none |
| react, react-dom, @types/react, @types/react-dom | 19.3.0 | yes | none |
| @vitejs/plugin-react | 4.7.0 | yes | none |
| @testing-library/react / jest-dom / dom (D-06) | 16.3.3 / 6.9.1 / 10.4.2 | yes | none |
| jsdom, globals | 26.1.0, 15.15.0 | yes | none |
| npm tooling (vite, typescript, eslint, @eslint/js, typescript-eslint, react-hooks, react-refresh, prettier, vitest, coverage-v8, playwright, stryker core and runner, jscpd) | as pinned | yes | Medium, accepted (`qs` via Stryker, dev only; D-05) |
| container images (postgres, eclipse-temurin, node, nginx, mailpit, semgrep, gitleaks, cloc) | as pinned | yes | not scanned (no image scanner in `tooling`) |

Licences: every npm package reports MIT or Apache-2.0 (`npm view <pkg> license`); Maven licences as listed in `tech-stack.md`.

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| — | nothing: every check passed or was covered by a pre-approved item or non-blocking decision | — | — |
