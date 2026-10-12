# Requirements check

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (story → criteria, and any gap; do not copy the criteria)

Inputs checked: `user-stories.md`, `acceptance-criteria.md`, `business-rules.md`, `scope.md`, `e2e-scenarios.md`.

## Story → criteria

| Story | Criteria (ids only) | Count | Business rules covered | E2E |
|---|---|---|---|---|
| US-001 | AC-001-01 … AC-001-08 | 8 | BR-01, BR-02, BR-03, BR-04, BR-05 | E2E-01, E2E-03 |
| US-002 | AC-002-01 … AC-002-07 | 7 | BR-01, BR-02, BR-03, BR-04, BR-05 | E2E-02 |
| US-003 | AC-003-01 … AC-003-03 | 3 | BR-04 | — |
| US-004 | AC-004-01 … AC-004-04 | 4 | BR-06 | E2E-01, E2E-02, E2E-03 |
| US-005 | AC-005-01 … AC-005-04 | 4 | BR-03, BR-07 | E2E-01, E2E-04 |
| US-006 | AC-006-01 … AC-006-03 | 3 | — | E2E-01 |
| US-007 | AC-007-01, AC-007-02 | 2 | BR-07 | E2E-01 |
| US-008 | AC-008-01 … AC-008-04 | 4 | BR-08 | E2E-04, E2E-05 |

Total 35 criteria, 5 end-to-end scenarios. Every story has at least one criterion; every criterion sits under exactly one story heading and cites rules or requirements; every business rule BR-01 … BR-08 is cited by at least one criterion.

## Open questions

OQ-01 … OQ-06 all have answers in `scope.md` (product-owner decisions; no record needed).

## Requirements without their own criterion

SR-03 (rate and size limits), NFR-02 (persistence across container recreation), NFR-04 (health and readiness), SR-02, SR-06 beyond AC-008-04, SR-08 (reverse proxy Host header): specified in phase 2, verified in phase 6, as `acceptance-criteria.md` states.

## Gaps and contradictions

| Gap | Inputs | Decision |
|---|---|---|
| `scope.md` has a stray line before the `# Scope` heading repeating the OQ answers; AC-006-03 cites `OQ-03` twice | `scope.md`, `acceptance-criteria.md` | D-09 (editorial, no behaviour impact) |
| AC-008-04 says "refused" without a status code | `acceptance-criteria.md` | D-10 |
| AC-005-03 says "an error (not 201)" without a status code or message | `acceptance-criteria.md` | D-11 |

No contradiction between the inputs was found.
