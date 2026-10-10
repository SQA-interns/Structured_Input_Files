# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: Docker Engine 29.8.1 installed, 29.8.0 pinned
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: blocking
- Trigger: `tech-stack.md` platform `docker` pins 29.8.0; `docker --version` and `docker info` (two methods) both report 29.8.1. Docker Compose 5.5.1 matches. All pinned images pulled and ran under 29.8.1.
- Options: 1. Accept Docker Engine 29.8.1 (patch release of the pinned minor) for this run; the pin stays authoritative (proposed default). 2. The human installs Docker Engine 29.8.0 and the check is re-run.
- Human response: none
- Resolution: pending

## D-02: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: non-blocking
- Trigger: `tech-stack.md` tooling `aldanial/cloc` pins tag `2.10`; the image resolves and runs, `cloc --version` inside it prints 1.98 (checked twice).
- Options: 1. Keep the pinned tag; it is authoritative and the same in every run (proposed default). 2. Change the tag (needs human approval).
- Human response: none
- Resolution: 1, per the `tech-stack.md` rule for pins whose tool reports another version.

## D-03: Dependency-Check analysers disabled
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: non-blocking
- Trigger: `dependency-check-maven` 12.1.0 enables the Sonatype OSS Index analyser by default, which needs Sonatype credentials not listed in `secrets.env.example`. The Node, Node Audit and RetireJS analysers would scan the frontend, which `npm audit` (the frontend's listed dependency-scan tool) already covers; the .NET assembly analyser does not apply.
- Options: 1. Disable OSS Index, Node, Node Audit, RetireJS and assembly analysers in `backend/pom.xml`; the NVD analyser (with `NVD_API_KEY`) and the CISA KEV list stay on (proposed default). 2. Ask for Sonatype credentials.
- Human response: none
- Resolution: 1. The scan completed with the NVD-based analysers.

## D-04: CVE-2025-7962 (CVSS 7.5) matched to angus-activation 2.0.3
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: blocking
- Trigger: Phase 0 Dependency-Check (`logs/phase0-backend-dependency-check.log`, JSON report) lists CVE-2025-7962 (SMTP injection in Jakarta Mail / Eclipse Angus Mail before 2.0.4) on `org.eclipse.angus:angus-activation:2.0.3`. Tool severity MEDIUM, NVD CVSS v3.1 base 7.5, which `severity-scale.md` maps to High. Evidence it does not apply: the match is the CPE `cpe:2.3:a:eclipse:angus_mail:*` (versionEndExcluding 2.0.4) applied to the activation jar by name similarity; the mail implementation on the classpath is `org.eclipse.angus:angus-mail:2.0.5` (fixed version), and `angus-activation` contains no SMTP code. SR-05 (header injection) is additionally enforced by the application in phase 4.
- Options: 1. Classify as a false positive (Low), add a documented suppression for this CPE on `angus-activation` only, keep the stack unchanged (proposed default). 2. Keep it as High and stop until the stack changes (no newer pin is needed: angus-mail is already fixed).
- Human response: none
- Resolution: pending

## D-05: CVE-2025-15104 matched to hibernate-validator 9.1.3.Final
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check lists CVE-2025-15104 (MEDIUM, CVSS 5.3) on `hibernate-validator-9.1.3.Final`. The CVE concerns Nu Html Checker (validator.nu); the match is the CPE `cpe:2.3:a:validator:validator:*`, a name collision with Hibernate Validator.
- Options: 1. Record as a false positive (Low) and suppress it for this jar only (proposed default). 2. Keep it as Medium.
- Human response: none
- Resolution: 1, pending review.

## D-06: npm audit reports qs (moderate) through Stryker
- Timestamp: 2026-10-10T22:17:58Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` (`logs/phase0-frontend-npm-audit.json`): 2 moderate (qs 6.15.1 via typed-rest-client 2.3.1 via `@stryker-mutator/core` 10.0.0). Dev-only mutation tooling, never shipped in the frontend bundle; no Critical or High.
- Options: 1. Accept as Medium for dev tooling and re-check in phase 6 (proposed default). 2. Override qs (adds an unlisted dependency pin).
- Human response: none
- Resolution: 1, pending review.

## D-07: Human answers to D-01 and D-04
- Timestamp: 2026-10-10T22:19:40Z
- Phase: 0
- Type: blocking
- Trigger: follow-up to D-01 and D-04 (records are append-only).
- Options: see D-01 and D-04.
- Human response: D-01: "go with option version 29.8.1"; D-04: "mark it as false positive" (2026-10-10T22:19:40Z).
- Resolution: D-01 resolved with option 1 (Docker Engine 29.8.1 accepted for this run). D-04 resolved with option 1 (CVE-2025-7962 on angus-activation classified Low, false positive; suppressed for that jar only in `backend/dependency-check-suppressions.xml`). D-05's suppression is added to the same file.

## D-08: OQ-01 option availability for students
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: `scope.md` OQ-01 unanswered; US-002 speaks of "the activities available to students", which suggests some options may be restricted.
- Options: 1. Every option in the configuration carries an audience list (external, student); omitted means both. The backend offers and accepts only options whose audience includes the registration type (proposed default). 2. All active options are available to every participant.
- Human response: none
- Resolution: 1, pending review. Without restrictions in the configuration it behaves like option 2.

## D-09: OQ-02 mandatory consents and wording
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-02 unanswered; BR-05 and SB-14 require explicit, never-preselected consents.
- Options: 1. Consents are defined in the same configuration as the options (identifier, wording, mandatory flag). The shipped configuration has one mandatory consent: processing of the entered personal data for organising the conference. Each given consent is stored with its timestamp (proposed default). 2. Hard-code one consent.
- Human response: none
- Resolution: 1, pending review; the wording must be approved by the product owner before production (listed in release notes).

## D-10: OQ-03 storage succeeds but an email fails
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-03 unanswered; `scope.md` priority 1 says storage comes before any notification.
- Options: 1. Emails are sent only after the database row and JSON copy are written; an email failure does not change the accepted registration or the success response; the failure is logged without personal data, and the registration stays available in the export and the JSON copy (proposed default). 2. Fail the request when an email fails.
- Human response: none
- Resolution: 1, pending review.

## D-11: OQ-04 options per category
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-04 unanswered.
- Options: 1. Any number of active options per category, each option at most once; a repeated identifier is rejected (proposed default). 2. At most one option per category.
- Human response: none
- Resolution: 1, pending review.

## D-12: OQ-05 second registration with the same email
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-05 unanswered.
- Options: 1. Reject a second registration with the same email (compared case-insensitively after trimming) with a field error on email; nothing is stored or sent (proposed default; conservative, prevents duplicates and mail flooding). 2. Allow duplicates.
- Human response: none
- Resolution: 1, pending review.

## D-13: OQ-06 retention of registrations and JSON copies
- Timestamp: 2026-10-10T22:20:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-06 unanswered; SB-13 needs a stated retention period.
- Options: 1. Keep registrations and JSON copies until the organizers delete them after the conference, at most 12 months after the conference; deletion is a manual operator task because deletion features are out of scope (proposed default). 2. Implement automatic deletion.
- Human response: none
- Resolution: 1, pending review; listed in the release notes.

## D-14: Field length limits and control characters
- Timestamp: 2026-10-10T22:20:59Z
- Phase: 1
- Type: non-blocking
- Trigger: BR-02/BR-03 give no maximum length; SR-05 requires that input cannot inject email headers or markup; SB-01 requires server-side validation.
- Options: 1. Every text field is at most 200 characters (email at most 254) after trimming; any control character (Unicode category Cc, including CR and LF) in a field is rejected with a field error; emails are plain text so markup is shown literally (proposed default). 2. Accept any length and strip control characters silently.
- Human response: none
- Resolution: 1, pending review.

## D-15: Frozen test AC-008-03 cannot pass with any implementation
- Timestamp: 2026-10-10T22:56:19Z
- Phase: 4
- Type: blocking
- Trigger: `backend/src/test/java/si/konferenca/registration/acceptance/RegistrationExportAcceptanceTest.java` (listed in `docs/03_acceptance-manifest.sha256`), helper `assertRefused`: `assertThat(response.body()).doesNotContain(new byte[] {'P', 'K', 3, 4})`. On a byte array AssertJ's `doesNotContain` fails when the body contains *any* of the bytes 'P', 'K', 3 or 4, not the ZIP signature as a sequence. `ac_008_03_plainHttpFromRemoteClientIsRefused` also asserts the body contains `HTTPS_REQUIRED`, which contains 'P', so the test contradicts itself. The implementation answers 403 with `{"error":"HTTPS_REQUIRED",...}` (no workbook), as AC-008-03 requires. The 401 cases pass only because their bodies happen to contain no 'P' or 'K'.
- Options: 1. The human approves replacing that line with a check of the first four bytes, `assertThat(java.util.Arrays.copyOf(response.body(), 4)).isNotEqualTo(new byte[] {'P', 'K', 3, 4})`, and the agent updates the manifest hash in a separate commit (proposed default). 2. Leave the test unchanged; AC-008-03 stays red and the phase 4 gate cannot pass.
- Human response: none
- Resolution: pending

## D-16: SpotBugs EI_EXPOSE_REP and EI_EXPOSE_REP2 excluded
- Timestamp: 2026-10-10T23:02:29Z
- Phase: 4
- Type: non-blocking
- Trigger: `spotbugs:check` (threshold Medium, effort Max) reported 29 EI_EXPOSE_REP/EI_EXPOSE_REP2 matches: record components of type `List` in response DTOs and value records (most already copied with `List.copyOf`) and constructor-injected collaborators (ports, Spring beans). None exposes mutable internal state; Low per `severity-scale.md` (theoretical / false positive).
- Options: 1. Exclude the two patterns in `backend/spotbugs-exclude.xml`, keep every other detector at threshold Medium (proposed default). 2. Wrap every list and collaborator in defensive copies to silence the detector.
- Human response: none
- Resolution: 1, pending review.

## D-17: Human answer to D-15
- Timestamp: 2026-10-10T23:05:00Z
- Phase: 4
- Type: blocking
- Trigger: follow-up to D-15 (records are append-only).
- Options: see D-15.
- Human response: "D-15 1, you can replace the line" (2026-10-10T23:05:00Z).
- Resolution: D-15 resolved with option 1. The line in `assertRefused` was replaced with the first-four-bytes check (formatted over two lines by google-java-format) and the manifest hash of `RegistrationExportAcceptanceTest.java` updated in its own commit.

## D-18: Frontend mutation score cannot be measured with the pinned Stryker and Vitest
- Timestamp: 2026-10-10T23:53:52Z
- Phase: 6
- Type: blocking
- Trigger: `npx stryker run` (`@stryker-mutator/core` and `vitest-runner` 10.0.0, `vitest` 5.0.3, all pinned in `tech-stack.md`) reports 24.2 % (66 killed, 40 timed out, 332 survived of 438; `logs/phase6-stryker.log`, `frontend/reports/mutation/mutation.json`) although line coverage is 100 % and the tests clearly detect many of the "surviving" mutants (e.g. `code < 0x7f` in `hasControlCharacter`, asserted by `validation.test.ts`). A probe with `--coverageAnalysis off` on `src/validation.ts` gives the same picture (29.7 %, `logs/phase6-stryker-probe.log`): only mutants evaluated at import time are killed. The runner 10.0.0 (latest on npm) is built against Vitest 4.1 and activates runtime mutants in a `beforeAll` hook of its setup file, which does not take effect under Vitest 5.0.3. The figure is a tool artefact, not a measurement.
- Options: 1. Record the frontend mutation score as not obtainable with the pinned tools (`null` with this reason in `run-log.json`, the invalid raw result kept in the logs), keep every pin, and list it as a known limitation; the threshold is "record only" (proposed default). 2. The human approves a tool change for mutation only (e.g. Vitest 4.1.x for the Stryker run), which changes `tech-stack.md` entries.
- Human response: none
- Resolution: pending
