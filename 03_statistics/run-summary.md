# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. Cost and token rows are filled by the post-run session (`metrics.md` section 2); until then write "pending".

## Run

- Run id: `kyuhi-single-agent-sdd-v005-opus-5-5-t1.5-20261012T1906Z-73c1`
- Model `claude-opus-5-5`, effort `medium`, template version 1.5, operator kyuhi, permission mode auto
- Start commit `029038d`; end commit: set by `stat.mjs phase 7 end` (`finalCommit` in `run-log.json`)
- Start 2026-10-12T19:06:27Z; cost and tokens: pending (post-run session)

## Time (human waiting excluded)

Minutes from the `stat.mjs` timestamps in `run-log.json`; phase 7 ends with the final `stat.mjs phase 7 end` after this file is committed.

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 Preflight & bootstrap | 9.6 | 1.9 (D-07, D-08: test-scope Critical/High dependency findings) | 7.7 |
| 1 Requirements | 0.4 | 0 | 0.4 |
| 2 Design | 5.1 | 0 | 5.1 |
| 3 Test design | 25.4 | 0 | 25.4 |
| 4 Build | 24.1 | 0 | 24.1 |
| 5 Unit tests | 28.9 | 0 | 28.9 |
| 6 Verify | 6.8 | 0 | 6.8 |
| 7 Release | ≈ 6 at writing (final value in `run-log.json`) | 0 | ≈ 6 |
| Total | ≈ 106 | 1.9 | ≈ 104 |

No wait was longer than one hour.

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 107: backend acceptance 92, frontend acceptance 10, end-to-end 5; all failed for behavioural reasons before the build; no frozen test changed |
| First complete run (phase 5, before any fix) | 113 passed, 0 failed (92 + 6 architecture backend, 10 frontend, 5 e2e) |
| Final run (phase 6) | 298 passed, 0 failed (backend 241, frontend 52, e2e 5) |
| Coverage, all tests | backend line 99.24 % / branch 98.80 % (JaCoCo); frontend line 100 % / branch 99.08 % (Vitest v8) |
| Mutation score | backend raw 91.44 %, adjusted 91.44 %, PIT 1.30.0, 267 killed, 0 equivalent, 292 total, scope `si.konferenca.registration.*` minus entry point; frontend raw 90.24 %, adjusted 90.24 %, Stryker 10.0.0, 370 killed, 0 equivalent, 410 valid (90.02 % with the compile-error mutant), scope `src/**/*.{ts,tsx}` minus tests and `main.tsx`; e2e not in the mutation runs (D-16) |
| Quality gate | met for both components and both thresholds at the first mutation run; 0 survivor rounds; tests at the first mutation run and at the end: backend 241, frontend 52; no shortfall |
| Findings as found | Critical 0, High 0, Medium 3, Low 5 (phase 6); in phase 0 the scanners reported test-scope Critical/High matches that the human classified Low (D-07, D-08) |
| Findings after triage | open Critical or High: 0; lowered by the human: D-07, D-08 |
| Fix loops | 0 (no finding needed a fix in phase 6) |
| Code (cloc) | backend production 1 770 / test 3 543 lines; frontend production 622 / test 1 369 lines |
| Duplication | backend 0 (PMD CPD); frontend 0.00 % (jscpd) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 92 / 0 | — | — | — | JUnit 6, MockMvc, Testcontainers |
| backend | integration | 5 / 0 | — | — | — | JUnit 6, MockMvc, Testcontainers |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 138 / 0 | 86.91 % | 96.80 % | — | JaCoCo 0.8.12 |
| backend | non-unit | 103 / 0 | 90.98 % | 74.40 % | — | JaCoCo 0.8.12 |
| backend | all levels | 241 / 0 | 99.24 % | 98.80 % | 91.44 % raw and adjusted, 267 of 292, `si.konferenca.registration.*`, all 241 JUnit tests | PIT 1.30.0 |
| frontend | acceptance | 10 / 0 | — | — | — | Vitest 4.1.11 |
| frontend | unit | 42 / 0 | 98.72 % | 92.66 % | — | Vitest v8 |
| frontend | non-unit | 10 / 0 | 84.07 % | 70.64 % | — | Vitest v8 |
| frontend | all levels | 52 / 0 | 100 % | 99.08 % | 90.24 % raw and adjusted, 370 of 410, `src/**/*.{ts,tsx}`, all 52 Vitest tests | Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| — | none | — | — | — | — |

## Commits

pending (post-run step); 67 commits at the time of writing.

## Decisions and human interventions

16 decisions: blocking 2 (security-finding: D-07, D-08), non-blocking 14 (dependency 2, security-finding 3, environment 1, requirement 4, other 4). Pending review by design: D-10, D-11, D-12.

| Intervention | Cause | Minutes |
|---|---|---|
| D-07, D-08 answered ("apply proposed default") | decision | 1.9 |

A pre-approved entry for test-scope-only scanner matches (Testcontainers CPE collision, shaded httpclient5 in `docker-java-transport-zerodep`) would have avoided both blocking decisions.

## Divergences

- Phase 4: commit `a43d21f` (US-003) exceeded the commit size guide without a stated reason (F-08).
- Phase 1: the decision records D-09 … D-11 carry a typed timestamp (19:17:00Z) instead of a clock value; the phase 1 boundaries themselves come from `stat.mjs`.

## What held the run back

1. Phase 5 (28.9 min) was dominated by the two mutation runs (PIT 13 min on the full Spring/Testcontainers suite, Stryker 7 min), run in parallel in the background.
2. Phase 3 (25.4 min) needed harness fixes before the freeze: PostgreSQL connection exhaustion by cached test contexts and one test that passed on the skeleton for the wrong reason (AC-005-03).
3. Phase 4 lost time on a platform defect hidden on Windows: JavaMail rewrote LF to CRLF in the JSON attachment, found only by the e2e run on Linux containers.

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
