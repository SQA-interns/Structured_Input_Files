# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. Cost and token rows are filled by the post-run session (`metrics.md` section 2); until then write "pending".

## Run

| Item | Value |
|---|---|
| Run id | kyuhi-single-agent-sdd-v006-opus-5-5-t1.6-20261012T2325Z-0b56 |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.6 |
| Start commit | 6bf4a55 |
| End commit | set by `stat.mjs phase 7 end` (`finalCommit`) |

## Time (human waiting excluded)

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 Preflight and bootstrap | 8.2 min | 0 | 8.2 min |
| 1 Requirements | 0.5 min | 0 | 0.5 min |
| 2 Design | 6.1 min | 0 | 6.1 min |
| 3 Test design | 18.3 min | 0 | 18.3 min |
| 4 Build | 20.4 min | 0 | 20.4 min |
| 5 Unit tests | 30.6 min | 0 | 30.6 min |
| 6 Verify | 9.7 min | 0 | 9.7 min |
| 7 Release | from `phase 7 end` | 0 | from `phase 7 end` |
| **Total (phases 0–6)** | **93.8 min** | **0** | **93.8 min** |

No wait for the human: no blocking decision was needed (pre-approved items PA-01 … PA-03 covered every scanner result).

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 99: backend acceptance 84, frontend acceptance 10, end-to-end 5; all failed for behavioural reasons on the skeleton (401, missing form, 502 from the API); no change to a frozen test |
| First complete run (phase 5, before any fix) | 99 passed, 0 failed |
| Final run (phase 6) | 290 passed, 0 failed (backend 216, frontend 69, e2e 5) |
| Coverage, all tests | backend line 99.32 % / branch 99.65 % (JaCoCo); frontend line 100 % / branch 99.03 % (v8) |
| Mutation score | backend raw 95.72 % = adjusted, PIT 1.30.0, 358 killed, 0 equivalent, 374 total, scope `si.konferenca.registration.**` without the entry point; frontend raw 94.83 % = adjusted, Stryker 10.0.0, 385 killed, 0 equivalent, 406 total, scope `src/**` without `main.tsx`; e2e tests not in mutation runs (D-14) |
| Quality gate | met for both: backend line and mutation met with 0 survivor rounds (216 tests at first mutation run and at the end); frontend line met, mutation 89.66 % on the first full run, met after 1 round (56 tests at first mutation run, 69 at the end); no shortfall |
| Findings as found | Critical 0, High 1, Medium 3, Low 5 |
| Findings after triage | open Critical or High: 0; High CVE-2025-7962 lowered by pre-approval PA-01 (D-03), no human decision needed |
| Fix loops | 0 (no Critical or High needed a fix in phase 6) |
| Code (cloc) | backend 2064 production / 3411 test lines; frontend 607 / 1717 |
| Duplication | backend 0 % (PMD CPD); frontend 0 % (jscpd) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 84 / 0 | — | — | — | JUnit 6, Testcontainers |
| backend | integration | 0 / 0 (acceptance tests run against real PostgreSQL and Mailpit) | — | — | — | — |
| backend | architecture | 5 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 127 / 0 | 92.88 % | 99.65 % | — | JUnit 6, Mockito, JaCoCo 0.8.12 |
| backend | non-unit | 84 / 0 | 87.68 % | 68.07 % | — | JaCoCo 0.8.12 |
| backend | all levels | 216 / 0 | 99.32 % | 99.65 % | 95.72 %, 358 of 374, `si.konferenca.registration.**`, all JUnit levels | PIT 1.30.0 |
| frontend | acceptance | 10 / 0 | — | — | — | Vitest 4.1.11 |
| frontend | unit | 59 / 0 | 100 % | 99.03 % | — | Vitest, coverage-v8 |
| frontend | non-unit | 10 / 0 | 74.85 % | 69.23 % | — | coverage-v8 |
| frontend | all levels | 69 / 0 | 100 % | 99.03 % | 94.83 %, 385 of 406, `src/**`, all Vitest levels | Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| — | none | — | — | — | — |

Defects found by tests before phase 6 and fixed: JSON attachment line endings (E2E-01, phase 4), reCAPTCHA string "true" accepted and display-name recipient rewritten (unit tests, phase 5).

## Commits

pending (post-run session).

## Decisions and human interventions

- Decisions: 14, all non-blocking. By category: environment 1 (D-01), dependency 3 (D-02, D-06, D-12), security-finding 4 (D-03, D-04, D-05, D-13), requirement 5 (D-07 … D-11), other 1 (D-14). Pending review: D-08 … D-11.
- Human interventions: none.
- Avoided by pre-approved answers: D-03 (PA-01), D-04 (PA-02), D-05 (PA-03).

## Divergences

1. Phase 0: bootstrap files of both components written before the first commit and committed in a burst; the backend build commit includes the vendored Maven wrapper scripts (650 lines).
2. Phases 3 and 5: three test commits over 400 changed lines (476, 445, 431).
3. Phases 3 and 5: two commit subjects over 72 characters (73, 75).
4. Phase 6: the runtime demonstration printed the `ORGANIZER_EMAILS` value to the session output and the demo log; redacted from the log before commit (F-09).

## What held the run back

1. Phase 5 took the most agent time (30.6 min): two full mutation runs on the frontend (first run 89.66 %, just under the gate, then a survivor round and a confirming run) and a PIT run that executes the Testcontainers acceptance suite for every mutant.
2. Phase 3/4 harness work: the e2e suite needs the whole Docker stack, and the first e2e run in phase 4 found a line-ending defect in the JSON attachment that only appears when the backend runs on Linux.
3. Windows specifics cost extra steps: UTF-8 request bodies passed as command-line arguments in the demo script, and the 260-character path limit in the clean-clone check.

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
