# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria (from `acceptance-criteria.md`) | Count | Business rules and answers involved | E2E scenarios |
|---|---|---|---|---|
| US-001 | AC-001-01 … AC-001-08 | 8 | BR-01..05, OQ-02, OQ-04, SR-01, SR-04 | E2E-01 |
| US-002 | AC-002-01 … AC-002-07 | 7 | BR-01..05, OQ-01, OQ-02, SR-01 | E2E-02 |
| US-003 | AC-003-01 … AC-003-03 | 3 | BR-04, AR-04 | (via E2E-01, E2E-02 form contents) |
| US-004 | AC-004-01 … AC-004-04 | 4 | BR-06, NFR-03, SB-07 | E2E-01, E2E-02, E2E-03 |
| US-005 | AC-005-01 … AC-005-04 | 4 | BR-03, BR-07, AR-05, NFR-01 | E2E-01, E2E-04 |
| US-006 | AC-006-01 … AC-006-03 | 3 | OQ-03, SR-05 | E2E-01 |
| US-007 | AC-007-01, AC-007-02 | 2 | BR-07, SR-07 | E2E-01 |
| US-008 | AC-008-01 … AC-008-04 | 4 | BR-08, SB-02, SR-06, NFR-01 | E2E-04, E2E-05 |
| **Total** | | **35** | | 5 scenarios |

## Gate checks

| Check | Result |
|---|---|
| Every story has at least one AC in the given list | Pass: 8 stories, 35 criteria, none without a criterion; the count matches the traceability table and the stated total |
| Every AC cites a story | Pass: every AC sits under its story heading and its ID carries the story number; the traceability table lists all 35 |
| Every open question has an answer in `scope.md` | Pass: OQ-01 … OQ-06 all answered in the "Open questions" table |
| Every e2e scenario names only existing criteria | Pass: E2E-01 … E2E-05 cite existing AC, NFR and DoD-P IDs |
| Gaps and contradictions recorded | D-07 (input formatting defects, no conflicting content) |

## Gaps

Behaviour the inputs leave open. None blocks phase 1; each is decided in phase 2 (specification), choosing the more conservative behaviour where a user would notice the difference (`working-rules.md`).

| # | Gap | Where it is decided |
|---|---|---|
| G-1 | `scope.md` starts with a stray line repeating the OQ-06…OQ-01 answers before the `# Scope` heading; `AC-006-03` cites `OQ-03` twice. The table answers and the criteria are unambiguous. | D-07 (table is authoritative) |
| G-2 | No maximum length is given for the text fields. | spec: field limits (SB-01, SR-03 body limit) |
| G-3 | "Valid email format" (BR-03) is not defined further. | spec: email rule |
| G-4 | The status code for the HTTPS-only refusal (AC-008-04) and for a storage failure (AC-005-03, "not 201") is not fixed. | spec: error model |
| G-5 | Rate-limit values and the request-size limit (SR-03) are not given. | spec: limits |
| G-6 | Whether emails are sent synchronously or after the response is not fixed; OQ-03 only fixes the order (after storage) and that failures keep the 201. | spec: notification flow |
| G-7 | The source of the organizer notification recipients, sender address and conference name is configuration (`environments.md`), but the email wording is not given. | spec: email content |
