# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `docker run --rm aldanial/cloc:2.10 --version` prints `1.98` (image created 2023-08-19); tech-stack.md pins tag `2.10`. The pin resolves and the tool runs.
- Options: 1. keep the pinned tag `aldanial/cloc:2.10` as authoritative and use it unchanged in every measurement (default); 2. ask for another tag.
- Human response: none
- Resolution: option 1 (tech-stack.md rule: a pin that resolves and runs but reports another version is non-blocking)

## D-02: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: first run of `dependency-check-maven` 12.1.0 (`02_output/logs/phase0-dependency-check.log`) ended with errors from the Sonatype OSS Index Analyzer, which needs credentials that `secrets.env.example` does not list.
- Options: 1. disable `ossindexAnalyzerEnabled` and scan with the NVD-based analysers (default); 2. ask for OSS Index credentials.
- Human response: none
- Resolution: option 1 (preflight step 6); re-run completed (`phase0-dependency-check-rerun.log`, BUILD SUCCESS)

## D-03: CVE-2025-7962 and CVE-2025-15104 suppressed (PA-01, PA-02)
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High by severity-scale.md) on `angus-activation-2.0.3.jar` and CVE-2025-15104 (CVSS 5.3) on `hibernate-validator-9.1.3.Final.jar`. Evidence: resolved `angus-mail` is 2.0.5 (`phase0-backend-deps.txt`); CVE-2025-15104 is the Nu Html Checker CPE collision.
- Options: 1. apply pre-approved.md PA-01 and PA-02: classify Low, false positive, suppress for those artifacts only (default).
- Human response: none (pre-approved in `01_project/00_setup/pre-approved.md`)
- Resolution: option 1; suppressions in `backend/dependency-check-suppressions.xml`

## D-04: npm audit Moderate advisories via @stryker-mutator/core (PA-03)
- Timestamp: 2026-10-11T09:09:21Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 Moderate (`qs` 6.15.1 via `typed-rest-client` 2.3.1 via `@stryker-mutator/core` 10.0.0); `npm audit --omit=dev` reports 0.
- Options: 1. apply pre-approved.md PA-03: accept, record as Medium, accepted (default).
- Human response: none (pre-approved)
- Resolution: option 1

## D-05: OQ-01 option availability by registration type
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-01 unanswered in `scope.md`; US-002 speaks of "activities available to students".
- Options: 1. every option carries a configured list of registration types it is available to (default: both), and the backend rejects an option not available to the type (default); 2. every active option is available to everyone.
- Human response: none
- Resolution: pending review (option 1: configuration-only, rejects rather than silently accepts; with the default of both types it behaves like option 2)

## D-06: OQ-02 mandatory consents and wording
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-02 unanswered; BR-05 requires mandatory consents, never preselected.
- Options: 1. consents are part of the conference configuration (id, wording, mandatory); the shipped configuration has one mandatory consent `privacy`: "I agree that my personal data is processed for organizing the conference and that it is shared with the conference organizers." (default); 2. hard-code consents.
- Human response: none
- Resolution: pending review (option 1); the wording must be confirmed by the product owner before production

## D-07: OQ-03 storage succeeds but an email fails
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-03 unanswered; scope priority 1: storage comes before any notification.
- Options: 1. the registration stays accepted (HTTP 201, confirmation shown); emails are sent only after the database row and JSON copy exist; a failure is logged with the registration id only (no personal data) so an operator can resend from the JSON copy (default); 2. roll back the registration when an email fails.
- Human response: none
- Resolution: pending review (option 1; option 2 would lose registrations, contrary to priority 1)

## D-08: OQ-04 number of options per category
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-04 unanswered.
- Options: 1. no per-category limit; each option at most once per registration, duplicates rejected (default); 2. at most one option per category.
- Human response: none
- Resolution: pending review (option 1; an invented limit could lock out legitimate users, a High severity example)

## D-09: OQ-05 second registration with the same email
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-05 unanswered.
- Options: 1. allowed; each registration is stored separately and organizers see both in the export (default); 2. reject a duplicate email.
- Human response: none
- Resolution: pending review (option 1: never loses a registration, and a rejection would reveal to anyone whether an email is registered)

## D-10: OQ-06 retention of registrations and JSON copies
- Timestamp: 2026-10-11T09:11:53Z
- Phase: 1
- Type: non-blocking
- Trigger: OQ-06 unanswered; SB-13 needs a stated retention period.
- Options: 1. keep until the operator deletes them; no automatic deletion in this release; release notes require the operator to delete the database rows and JSON copies no later than 12 months after the conference (default); 2. implement automatic deletion.
- Human response: none
- Resolution: pending review (option 1; deletion features are not in scope)

## D-11: add @types/node 24.13.0 (dev only)
- Timestamp: 2026-10-11T09:31:01Z
- Phase: 3
- Type: non-blocking
- Trigger: `tsc --noEmit` fails on the Playwright config and end-to-end helpers (`process`, `Buffer`, `node:zlib`), because Node type definitions are not in `tech-stack.md`.
- Options: 1. add `@types/node` 24.13.0 (exact, matches the Node 24.13.0 pin, MIT, dev dependency, not in the bundle) and enable it in `tsconfig.json` (default); 2. exclude end-to-end files from type checking.
- Human response: none
- Resolution: option 1 (tech-stack.md rule for unlisted dev tooling); scanned in phase 6 like any other dependency
