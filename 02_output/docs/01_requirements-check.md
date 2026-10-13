# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

## Story → criteria

| Story | Criteria (`acceptance-criteria.md`) | Count | E2E scenarios | Rules cited |
|---|---|---|---|---|
| US-001 | AC-001-01 … AC-001-08 | 8 | E2E-01 | BR-01..05, SR-01, SR-04, OQ-02, OQ-04 |
| US-002 | AC-002-01 … AC-002-07 | 7 | E2E-02 | BR-01..05, SR-01, OQ-01, OQ-02 |
| US-003 | AC-003-01 … AC-003-03 | 3 | (E2E-01, E2E-02 use the shipped configuration) | BR-04, AR-04 |
| US-004 | AC-004-01 … AC-004-04 | 4 | E2E-01, E2E-02, E2E-03 | BR-06, NFR-03, SB-07 |
| US-005 | AC-005-01 … AC-005-04 | 4 | E2E-01, E2E-04 | BR-03, BR-07, AR-05, NFR-01 |
| US-006 | AC-006-01 … AC-006-03 | 3 | E2E-01 | SR-05, OQ-03 |
| US-007 | AC-007-01, AC-007-02 | 2 | E2E-01 | BR-07, SR-07 |
| US-008 | AC-008-01 … AC-008-04 | 4 | E2E-04, E2E-05 | BR-08, SB-02, SR-06, NFR-01 |

Total 35 criteria (35 distinct `AC-` ids in the input), 5 end-to-end scenarios.

## Gate checks

| Check | Result |
|---|---|
| Every story has ≥ 1 AC | Pass: US-001 … US-008 each have 2-8. |
| Every AC cites a story | Pass: every id carries its story number and sits under its story; no orphan id. |
| Every open question answered in `scope.md` | Pass: OQ-01 … OQ-06 all have an answer. |
| Every BR is referenced by at least one AC | Pass: BR-01 … BR-08. |
| Requirements without their own AC | SR-03, NFR-02, NFR-04 (named in `acceptance-criteria.md`), plus SR-02, SR-08 and the SB controls: mapped in the specification (phase 2), verified in phase 6. |

## Gaps and contradictions

| # | Finding | Handling |
|---|---|---|
| G-1 | `scope.md` begins with an unformatted line before its title that repeats the six open-question answers in reverse order, separated by `\|`. Its text is identical to the answers in the open-questions table; no contradiction. | D-09 (non-blocking): the table is authoritative. |
| G-2 | AC-006-03 cites `OQ-03` twice in its Refs column. Cosmetic; meaning unaffected. | No decision needed. |
| G-3 | AC-008-04 says the export over plain HTTP from a non-localhost client is "refused" but does not name the status code; AC-008-02 fixes 401 for missing or wrong credentials. | D-10 (non-blocking, pending review): refuse with HTTP 403 and a generic error body before any credential is evaluated, whether or not credentials are valid. |
| G-4 | "Rejected" requires field errors; for a missing or rejected reCAPTCHA token (AC-001-07, AC-002-06) the field is not a form field. | Specification (phase 2) names the field `recaptchaToken` in the error list; no behaviour choice involved. |
| G-5 | AC-005-03 requires "an error (not 201)" without a status code. | Specification: HTTP 500 with a generic message (SB-07). Not a user-visible choice beyond "error". |
| G-6 | OQ-01 allows an option to be unavailable to either type; AC-002-04 tests only the student side. | Specification applies the same check to external submissions (OQ-01 answer: "the backend rejects an option that is not available to the registration type"). |
| G-7 | SR-03 gives no numeric rate or body-size limits. | Specification sets configurable defaults (`environments.md`: defaults allowed). |
