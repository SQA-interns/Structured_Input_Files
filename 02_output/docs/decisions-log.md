# Decisions log

> Written in: every phase · Format: `general/working-rules.md` ("Decision record") · Agent: appends only

## D-01: cloc image tag 2.10 reports version 1.98
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: `aldanial/cloc:2.10` (tech-stack.md, tooling) resolves and runs, but `cloc --version` prints 1.98.
- Options: 1. keep the pinned image tag, which stays authoritative, and record the reported version (default); 2. ask for another tag.
- Human response: none
- Resolution: 1 (tech-stack.md rule: a pin that resolves and runs but reports another version is non-blocking)

## D-02: Dependency-Check OSS Index analyser disabled
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: the OSS Index analyser of `org.owasp:dependency-check-maven` 12.1.0 needs Sonatype OSS Index credentials, which `secrets.env.example` does not list. The scan runs with the NVD analysers (`NVD_API_KEY`) and completes.
- Options: 1. disable only the OSS Index analyser (`ossindexAnalyzerEnabled=false`) and scan with the remaining analysers (default); 2. ask for OSS Index credentials.
- Human response: none
- Resolution: 1 (preflight skill step 6)

## D-03: CVE-2025-7962 on angus-activation (PA-01)
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-7962 (CVSS 7.5, High) on `org.eclipse.angus:angus-activation` 2.0.3; the shipped `angus-mail` is 2.0.5 (dependency tree, `out/logs/00_backend-dependency-check-report.json`). Matches PA-01 exactly.
- Options: 1. apply PA-01: classify Low (false positive), suppress for that artifact only (default).
- Human response: none (pre-approved, `project/00_setup/pre-approved.md` PA-01)
- Resolution: 1; suppression in `backend/dependency-check-suppressions.xml`

## D-04: CVE-2025-15104 on hibernate-validator (PA-02)
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: Dependency-Check reports CVE-2025-15104 (CVSS 5.3) on `hibernate-validator` 9.1.3.Final through the CPE `validator:validator` (Nu Html Checker). Matches PA-02 exactly.
- Options: 1. apply PA-02: classify Low (false positive), suppress for that artifact only (default).
- Human response: none (pre-approved, PA-02)
- Resolution: 1; suppression in `backend/dependency-check-suppressions.xml`

## D-05: npm audit moderate advisories on qs through Stryker (PA-03)
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: `npm audit` reports 2 moderate advisories on `qs` 6.15.1, reached only through `@stryker-mutator/core` > `typed-rest-client`; `npm audit --omit=dev` reports 0 vulnerabilities. Matches PA-03.
- Options: 1. apply PA-03: accept, record as Medium, accepted (default).
- Human response: none (pre-approved, PA-03)
- Resolution: 1

## D-06: add @testing-library/dom 10.4.2 (dev only)
- Timestamp: 2026-10-12T23:32:33Z
- Phase: 0
- Type: non-blocking
- Trigger: `@testing-library/react` 16.3.3 (tech-stack.md) declares the required peer dependency `@testing-library/dom` ^10.0.0, which tech-stack.md does not list. Without it the component tests cannot run.
- Options: 1. add `@testing-library/dom` 10.4.2 (MIT, latest 10.x) as an exact dev dependency, scanned with the rest (default); 2. ask for a tech-stack change.
- Human response: none
- Resolution: 1 (tech-stack.md rule for unlisted dev-only tooling)

## D-07: formatting defects in the requirement inputs
- Timestamp: 2026-10-12T23:33:53Z
- Phase: 1
- Type: non-blocking
- Trigger: `project/01_requirements/scope.md` begins with a stray line holding the OQ-06 … OQ-01 answers in reverse order before the `# Scope` heading; `acceptance-criteria.md` AC-006-03 cites `OQ-03, OQ-03`. The repeated text matches the "Open questions" table word for word; no answer conflicts.
- Options: 1. treat the "Open questions" table and the criteria as authoritative, ignore the stray line and the duplicate reference, leave `01_input/` unchanged (default); 2. ask the product owner to fix the files.
- Human response: none
- Resolution: 1

## D-08: field limits and strict request validation
- Timestamp: 2026-10-12T23:40:00Z
- Phase: 2
- Type: non-blocking
- Trigger: the inputs give no length limits and do not say what happens to fields of the other registration type or unknown properties (requirements check G-2, G-3; BR-01, SB-01).
- Options: 1. limits 100 (names), 254 (email), 200 (organization, institution, programme), 50 (student ID), 100 option ids; fields of the other type must be empty; unknown JSON properties rejected; email pattern `^[^\s@]+@[^\s@]+\.[^\s@]+$` (default, conservative); 2. no limits, ignore extra fields.
- Human response: none
- Resolution: pending review (option 1, spec 4.2)

## D-09: status codes for HTTPS-only refusal and storage failure
- Timestamp: 2026-10-12T23:40:00Z
- Phase: 2
- Type: non-blocking
- Trigger: AC-008-04 says "refused" and AC-005-03 says "an error (not 201)" without a status (G-4).
- Options: 1. 403 "HTTPS is required." before credentials are evaluated; 500 with a general message for storage failures (default); 2. 401 / 503.
- Human response: none
- Resolution: pending review (option 1, spec 4.3, 10)

## D-10: rate-limit values and request-size limit
- Timestamp: 2026-10-12T23:40:00Z
- Phase: 2
- Type: non-blocking
- Trigger: SR-03 requires per-client rate limits and a body-size limit without values (G-5). A limit too low would lock out legitimate users behind one address (severity scale, High example).
- Options: 1. per client and minute: form config 120, registrations 20, organizer export 10 (also limits failed logins); body ≤ 16 KiB; all configurable (default); 2. stricter values (risk of locking out a shared campus address).
- Human response: none
- Resolution: pending review (option 1, spec 11)

## D-11: email delivery form
- Timestamp: 2026-10-12T23:40:00Z
- Phase: 2
- Type: non-blocking
- Trigger: OQ-03 fixes the order and failure handling of emails, not their form (G-6, G-7).
- Options: 1. send synchronously after the commit, before the 201 answer, with 5 s timeouts; plain text only; one organizer message per address (addresses not disclosed to each other) (default); 2. asynchronous sending, one message to all organizers.
- Human response: none
- Resolution: pending review (option 1, spec 8, contract emails.md)
