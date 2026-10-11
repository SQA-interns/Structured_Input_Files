# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. Cost and token rows are filled by the post-run session (`metrics.md` section 2); until then write "pending".

## Run

| Item | Value |
|---|---|
| Run id | `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261010T2210Z-7c3c` |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.3 |
| Start | 2026-10-10T22:10:56Z, commit `af81ef5` |
| End | set by `stat.mjs phase 7 end` (end time and final commit in `run-log.json`) |
| Tokens, cost | pending (post-run session) |

## Time (human waiting excluded)

Minutes from the `stat.mjs` timestamps; phase 7 measured up to the writing of this summary (its final end is in `run-log.json`).

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 Preflight and bootstrap | 9.7 | 1.2 (D-01/D-04: Docker version, CVE classification) | 8.6 |
| 1 Requirements | 0.9 | 0 | 0.9 |
| 2 Design | 5.5 | 0 | 5.5 |
| 3 Test design | 13.2 | 0 | 13.2 |
| 4 Build | 26.6 | 2.0 (D-15: frozen test defect) | 24.6 |
| 5 Unit tests | 8.7 | 0 | 8.7 |
| 6 Verify | 69.2 | 0.9 (D-18: frontend mutation tooling) | 68.2 |
| 7 Release | ≈ 8 (at writing) | 0 | ≈ 8 |
| **Total** | **≈ 142** | **4.1** | **≈ 138** |

No wait exceeded one hour.

## Outcome

| Measure | Value |
|---|---|
| Acceptance criteria | 35, every one with at least one frozen test and one commit |
| Frozen tests | 98 (backend acceptance 81, frontend acceptance 12, e2e 5); 1 change, human-approved (D-15/D-17: self-contradictory assertion) |
| First complete run (phase 5) | 240 passed, 4 failed — all 4 defects in new non-frozen tests (CSV quote character, non-web Spring context); 0 implementation defects |
| Final run (phase 6) | 263 passed, 0 failed (unit 155, integration 3, acceptance 93, architecture 7, e2e 5) |
| Findings as found | Critical 0, High 0, Medium 4, Low 8 |
| Open at the end | Critical 0, High 0, Medium 0 (F-04 dev-only `qs` advisory accepted with D-06) |

## Quality measures

| Component | Production / test LOC (cloc) | Line coverage unit / non-unit / all | Branch coverage unit / non-unit / all | Mutation |
|---|---|---|---|---|
| backend | 2296 / 3241 | 74.9 / 90.7 / 97.7 % | 86.7 / 69.0 / 93.0 % | 95.4 % (374 of 392; covered 98.2 %), PIT 1.30.0, scope `si.konferenca.registration.**` minus entry point, full JUnit suite incl. Testcontainers acceptance |
| frontend | 607 / 998 | 97.6 / 92.4 / 100 % | 92.6 / 81.0 / 97.5 % | 84.7 % (371 of 438), Stryker 10.0.0 with Vitest 4.1.11, scope `src/**` minus tests, `main.tsx`, `setupTests.ts`, `*.d.ts`, all Vitest tests (e2e not runnable under Stryker) |

Duplication 0 % (PMD CPD, jscpd). Complexity: not measured (no tool in `tech-stack.md`). Frontend non-unit coverage covers the Vitest acceptance tests only.

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 | security-config | no | 2.7 | nginx no longer forwards the client `Host` header |
| 2 | F-07, F-08 | security-config | no | 12 | rate limit for every public `/api` request; rate-limiter cleanup test |
| 3 | F-06 | tooling | yes (D-18) | 17 | Vitest 4.1.11 so Stryker activates mutants |
| 4 | F-10 | test-gap | no | 4 | email TLD test cases |
| 5 | F-03 | process | no | 1.5 | redact an `.env` value from the demo log before commit |

Loop 3 could have been prevented by a template rule: check in phase 0 that the mutation tool really kills a known mutant with the pinned test runner (Stryker 10 vs Vitest 5 incompatibility).

## Commits

pending (post-run session: `commitSummary`).

## Decisions and human interventions

| Type | Category | Count |
|---|---|---|
| blocking | environment | 1 (D-01) |
| blocking | security-finding | 1 (D-04) |
| blocking | frozen-test | 2 (D-15, D-17) |
| blocking | dependency | 1 (D-19) |
| blocking | other | 2 (D-07 answer record, D-18) |
| non-blocking | requirement | 7 (D-08..D-14) |
| non-blocking | security-finding | 2 (D-05, D-06) |
| non-blocking | dependency | 2 (D-03, D-20) |
| non-blocking | environment | 1 (D-02) |
| non-blocking | other | 1 (D-16) |

| Intervention | Cause | Minutes |
|---|---|---|
| Phase 0: Docker 29.8.1 and CVE-2025-7962 classification | decision (D-01, D-04) | 1.2 |
| Phase 4: frozen test AC-008-03 | decision (D-15) | 2.0 |
| Phase 6: frontend mutation tooling | decision (D-18) | 0.9 |

Avoidable with pre-approved answers in the inputs: the six open questions OQ-01..OQ-06 (D-08..D-13); a rule for patch-level host tool differences (D-01); a pre-approved policy for CPE-mismatch false positives (D-04).

## Divergences

| Phase | Reference | Divergence |
|---|---|---|
| 0 | `tech-stack.md` docker | Docker Engine 29.8.1 instead of 29.8.0 (human-approved, D-01/D-07) |
| 4 | `working-rules.md` commit size | Two commits over the size guide without a stated reason (`bc0f68d` 21 files, `efd17db` 675 lines) |
| 6 | `tech-stack.md` vitest, @vitest/coverage-v8 | 4.1.11 instead of 5.0.3 (human-approved 4.1.x, D-18/D-19/D-20) |

## What held the run back

1. Phase 6 took about half of the agent time (68 of ≈ 138 minutes), most of it mutation testing: PIT over the full suite with Testcontainers acceptance tests ran twice for about 30 minutes each, and Stryker ran four times because of the Vitest 5 incompatibility.
2. Harness friction on Windows Git Bash (path conversion, argument encoding, long paths) cost several re-runs of the runtime demonstration and the clean-clone check.
3. A self-contradictory frozen assertion (AssertJ byte-array `doesNotContain`) blocked the phase 4 gate until the human approved the change.

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
