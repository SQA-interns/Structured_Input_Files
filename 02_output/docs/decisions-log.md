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
