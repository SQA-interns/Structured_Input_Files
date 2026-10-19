# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria | Count | Rules cited |
|---|---|---|---|
| US-001 | AC-001-01..08 | 8 | BR-01..05, SR-01, SR-04, OQ-02, OQ-04 |
| US-002 | AC-002-01..07 | 7 | BR-01..05, SR-01, OQ-01, OQ-02 |
| US-003 | AC-003-01..03 | 3 | BR-04, AR-04 |
| US-004 | AC-004-01..04 | 4 | BR-06, NFR-03, SB-07 |
| US-005 | AC-005-01..04 | 4 | BR-03, BR-07, AR-05, NFR-01 |
| US-006 | AC-006-01..03 | 3 | SR-05, OQ-03 |
| US-007 | AC-007-01..02 | 2 | BR-07, SR-07 |
| US-008 | AC-008-01..04 | 4 | BR-08, SB-02, SR-06, NFR-01 |
| **Total** | | **35** | matches the stated total |

Every story has at least one criterion; every criterion sits under exactly one story section and cites its rules. E2E-01..05 cite only existing criteria and DoD-P01..P04. OQ-01..06 all have an answer in `scope.md`.

## Requirements without their own criterion

Stated by `acceptance-criteria.md` and carried to phase 2 (control or target) and phase 6 (runtime demonstration): SR-02, SR-03, SR-08, NFR-02, NFR-04, BR-08 transport (SR-06 has AC-008-04), SB-10 headers, SB-11 non-root containers.

## Gaps and inconsistencies

| # | Where | Observation | Handling |
|---|---|---|---|
| G-1 | `business-rules.md`, last line after the glossary | A stray table fragment repeats the answers of OQ-06, OQ-05, OQ-04, OQ-03, OQ-02 and OQ-01 without a header. It agrees with `scope.md`. | D-06: `scope.md` answers are authoritative; the fragment adds nothing |
| G-2 | AC-006-03, Refs | Cites `OQ-03` twice. | cosmetic; no effect |
| G-3 | AC-008-04 | "refused" without a status code (AC-008-02 fixes 401 only for missing or wrong credentials). | phase 2 picks the status (conservative choice, pending review) |
| G-4 | SR-03 | No numbers for the rate limits or the body-size limit. | phase 2 sets configurable defaults (pending review) |
| G-5 | AC-005-03, AR-05 | Order between database commit and JSON copy is not fixed beyond "both before success". | phase 2 declares the transaction order (JSON copy written inside the database transaction, rolled back on failure) |
