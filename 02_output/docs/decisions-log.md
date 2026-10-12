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

## D-10: OQ-01 options per registration type
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: `scope.md` OQ-01 unanswered; US-002 speaks of "activities available to students".
- Options: 1. Every option is configured with the registration types that may select it (default: both); a form shows only options open to its type, and the backend rejects any other like an inactive option (proposed, conservative: rejects rather than accepts). 2. All active options are open to both types.
- Human response: none
- Resolution: option 1; pending review

## D-11: OQ-02 mandatory consents
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-02 unanswered; BR-05, SB-14 and the consent row in `security-requirements.md` need at least one consent.
- Options: 1. One mandatory consent on both forms: processing of the entered personal data for organising the conference, including the emails to the participant and organizers; checkbox unchecked by default; the wording comes from configuration so it can be corrected without code; stored with its timestamp (proposed). 2. Additional optional consents (for example photos, newsletter): not chosen, no requirement names them.
- Human response: none
- Resolution: option 1; pending review (wording must be approved by the organizer)

## D-12: OQ-03 email failure after storage
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-03 unanswered; `scope.md` priority 1 says storage comes before notification.
- Options: 1. The registration stays accepted and the participant sees the confirmation; each failed email is logged without personal data (registration id and email kind only); no automatic retry (proposed: never loses or duplicates a registration). 2. Report failure to the participant (invites duplicate submissions of an already stored registration).
- Human response: none
- Resolution: option 1; pending review

## D-13: OQ-04 options per category
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-04 unanswered.
- Options: 1. Any number of distinct active options per category, including none; the same option id twice in one registration is rejected (proposed: adds no limit the requirements do not state). 2. At most one option per category.
- Human response: none
- Resolution: option 1; pending review

## D-14: OQ-05 second registration with the same email
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-05 unanswered.
- Options: 1. Reject a registration whose email (trimmed, case-insensitive) is already registered, of either type, with a field error on the email; nothing is stored or sent (proposed, conservative: no duplicate records, no repeated emails to one address). 2. Allow duplicates.
- Human response: none
- Resolution: option 1; pending review

## D-15: OQ-06 retention of registrations and JSON copies
- Timestamp: 2026-10-12T15:46:06Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-06 unanswered; SB-13 needs a retention period per personal-data item.
- Options: 1. All registration data and JSON copies are kept until 12 months after the conference and then deleted by the organizer; the system offers no deletion feature (out of scope) and the release notes list the deletion as a manual task (proposed). 2. Keep indefinitely (fails SB-13).
- Human response: none
- Resolution: option 1; pending review

## D-16: Redocly CLI for OpenAPI validation
- Timestamp: 2026-10-12T15:51:43Z
- Phase: 2
- Type: non-blocking
- Trigger: the phase 2 gate needs the contracts validated with a parser; `tech-stack.md` lists no OpenAPI parser.
- Options: 1. Run `@redocly/cli` 2.62.1 (MIT) through `npx`, dev-only, not added to any component; JSON schemas checked with the host's Python `jsonschema` (Draft 2020-12 meta-schema plus example documents) (chosen). 2. Add a validator dependency to a component.
- Human response: none
- Resolution: option 1; scanned in phase 6 like any other tool

## D-17: User interface language is English
- Timestamp: 2026-10-12T15:51:43Z
- Phase: 2
- Type: non-blocking
- Trigger: no input states the UI or email language; participants may be Slovenian or international.
- Options: 1. English for all UI text and emails, Unicode content unchanged (proposed). 2. Slovenian. 3. Both.
- Human response: none
- Resolution: option 1; pending review

## D-18: Field length limits
- Timestamp: 2026-10-12T15:51:43Z
- Phase: 2
- Type: non-blocking
- Trigger: `business-rules.md` gives no maximum lengths; unbounded fields weaken SR-03 and the database schema.
- Options: 1. Text fields at most 200 characters, email at most 254 (RFC 5321), consent wording at most 1000 (proposed). 2. No limits.
- Human response: none
- Resolution: option 1; pending review

## D-19: Rate-limit and request-size defaults
- Timestamp: 2026-10-12T15:51:43Z
- Phase: 2
- Type: non-blocking
- Trigger: SR-03 and SB-06 require limits without values.
- Options: 1. Per client IP and minute: 10 registrations, 120 reads of options/settings, 10 organizer requests (failed logins included); request body at most 16 KiB; all configurable (proposed). 2. Other values.
- Human response: none
- Resolution: option 1; pending review

## D-20: Option changes take effect after a backend restart
- Timestamp: 2026-10-12T15:51:43Z
- Phase: 2
- Type: non-blocking
- Trigger: AR-04 leaves the mechanism to phase 2.
- Options: 1. JSON file validated at startup; a change needs a restart, and an invalid file stops the startup (proposed, conservative: a broken file never goes live). 2. Reload while running.
- Human response: none
- Resolution: option 1; pending review

## D-21: SpotBugs EI_EXPOSE_REP2 excluded for injected collaborators
- Timestamp: 2026-10-12T16:23:07Z
- Phase: 4
- Type: non-blocking
- Trigger: SpotBugs reports EI_EXPOSE_REP2 (Medium) for every constructor that stores an injected collaborator (ports, services, `TransactionTemplate`).
- Options: 1. Exclude only EI_EXPOSE_REP2 in `application`, `infrastructure`, `web` and `config` (`backend/spotbugs-exclude.xml`); domain records stay fully checked (chosen). 2. Copy collaborators (not possible for shared beans). 3. Accept failing static analysis (breaks DoD-02).
- Human response: none
- Resolution: option 1; pending review

## D-22: Compose cannot parse the repository .env
- Timestamp: 2026-10-12T18:55:19Z
- Phase: 4
- Type: non-blocking
- Trigger: `docker compose --env-file ../.env` fails: line 2 of `.env` is free text, neither `KEY=value` nor a comment. The agent does not read or edit `.env`.
- Options: 1. Start the stack with `02_output/scripts/compose.sh`, which exports only the keys compose needs with the `sed` method of `working-rules.md` (chosen; works with any `.env`). 2. Ask the human to turn line 2 into a comment (recommended anyway, so `--env-file` also works).
- Human response: none
- Resolution: option 1; pending review (human may also fix line 2)

## D-23: Backend mutation testing runs unit and architecture tests only
- Timestamp: 2026-10-12T19:03:38Z
- Phase: 6
- Type: non-blocking
- Trigger: `measurement-scope.md` asks for the full suite per mutant. The backend acceptance and integration tests start the Spring application with PostgreSQL and Mailpit containers (about 15 s per context, 59 tests); PIT would need hours to days for every mutant.
- Options: 1. PIT over all production code (scope unchanged, entry point excluded) with the unit and architecture tests (`excludedTestClasses` acceptance and integration); report `score` and `scoreCovered`; list the tests run (chosen). 2. Full suite per mutant (infeasible in the run).
- Human response: none
- Resolution: option 1; pending review

## D-24: Frontend mutation testing uses Stryker's command runner
- Timestamp: 2026-10-12T19:22:52Z
- Phase: 6
- Type: non-blocking
- Trigger: with `@stryker-mutator/vitest-runner` 10.0.0 (latest) and `vitest` 5.0.3 (D-08) Stryker ran 0.85-1.03 tests per mutant and reported mutants as surviving that unit tests assert directly (score 16.9 %); in debug mode the runner crashes serialising Vitest 5's configuration (`resolvedProjects -> viteConfig -> test` is circular). The measurement is invalid.
- Options: 1. Keep `@stryker-mutator/core` 10.0.0 and switch to its built-in `command` test runner (`npx vitest run` per mutant, `coverageAnalysis` off): same scope, full Vitest suite, only pinned tools (chosen). 2. Change the runner or Vitest version (blocking, needs `tech-stack.md` approval).
- Human response: none
- Resolution: option 1; the invalid vitest-runner results are kept in `out/logs/phase6-mutation-frontend*.log` and not reported as the score; pending review

## D-25: Semgrep "use of basic authentication" (High) on the organizer export
- Timestamp: 2026-10-12T19:32:27Z
- Phase: 6
- Type: blocking
- Trigger: Semgrep rule `yaml.openapi.security.use-of-basic-authentication` (ERROR → High per `severity-scale.md`) on `docs/02_contracts/openapi.yaml` (`organizerBasic`), finding F-06. HTTP Basic is the organizer mechanism chosen in phase 2 (S9) for the one organizer role and one export operation (`security-requirements.md`: identity providers out of scope; ASVS Level 1).
- Evidence for lowering: credentials are never accepted over plain HTTP from another host (SR-06, `OrganizerHttpsFilter`, integration test `organizerOverPlainHttpFromAnotherHostIsRefusedBeforeAuthentication`); TLS is terminated by the reverse proxy in production; the password is kept only as a BCrypt hash (SB-03); organizer requests including failed logins are rate limited (SR-03, SB-06); sessions are stateless, so no cookie or CSRF surface; the only protected operation is a read-only export.
- Options: 1. Lower F-06 to Low and accept HTTP Basic as specified, with the controls above (proposed default). 2. Keep it High and replace HTTP Basic with another mechanism (form login with session, or OIDC, which is out of scope) before release.
- Human response: none
- Resolution: pending
