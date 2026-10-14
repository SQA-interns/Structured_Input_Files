# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
| Item | Value |
|---|---|
| Run id | tanej-single-agent-sdd-v001-opus-5-5-t1.7-20261013T1947Z-3bcb |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.7 |
| Start commit | 425cab9 |
| End commit | 74b347f |
| Start, end | 2026-10-13T19:47:24Z, 2026-10-14T07:02:30Z |
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 19 min | 5 min | 14 min |
| 1 requirements | 1 min | 0 min | 1 min |
| 2 design | 7 min | 0 min | 7 min |
| 3 test-design | 35 min | 0 min | 35 min |
| 4 build | 28 min | 0 min | 28 min |
| 5 unit-tests | 38 min | 0 min | 38 min |
| 6 verify | 28 min | 10 min | 18 min |
| 7 release | 8 h 39 min | 8 h 32 min | 7 min |
| **Total** | **11 h 15 min** | **8 h 47 min** | **2 h 28 min** |

Waits over one hour: 1 (see the report).
<!-- /auto:time -->

## Outcome

<!-- One row per measure, values from `run-log.json`; keep the rows and their order. Goal: final run all green, no open Critical or High; always say what a coverage or mutation figure covers. -->

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 92: backend acceptance 80 (JUnit), frontend acceptance 7 (Vitest), e2e 5 (Playwright); all failed for behavioural reasons on the skeleton except the AC-003-03 harness control; AC-005-03 tightened before the freeze; no frozen-test change after the freeze |
| First complete run (phase 5, before any fix) | 92 passed, 0 failed |
| Final run (phase 6) | 264 passed, 0 failed (backend 203, frontend 56, e2e 5); repeated from a clean clone in phase 7 |
| Coverage, all tests | backend line 98.19 % / branch 97.92 % (JaCoCo, JUnit suite); frontend line 100 % / branch 98.34 % (Vitest v8, Vitest suite); e2e not instrumented (D-14) |
| Mutation score | backend PIT 1.30.0: raw 90.54 % = adjusted (335 killed incl. 1 timeout, 0 equivalent, 370 total), scope `si.konferenca.registration.*` minus entry point; frontend Stryker 10.0.0: raw 92.58 % = adjusted (412 killed, 0 equivalent, 445 total), scope `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts` |
| Quality gate | met for both: backend line and mutation met at the first full run (0 rounds; 200 tests at first run, 203 at the end); frontend line met, mutation met after 1 survivor round (40 tests at first run, 56 at the end); no shortfall |
| Findings as found | Critical 0, High 2, Medium 4, Low 7 |
| Findings after triage | open Critical 0, High 0; both High pre-approved (F-02 PA-04/D-15, F-08 PA-01/D-06); no finding lowered by a human |
| Fix loops | 1 (F-01, 16.9 min) |
| Code (cloc) | backend 2 190 / 2 703; frontend 711 / 1 461 (production / test lines, cloc image 2.10) |
| Duplication | backend PMD CPD (100 tokens): 0; frontend jscpd 5.4.0: 0.37 % (1 clone, 6 lines, test code) |

## Test measures

<!-- One row per component and level, from `docs/03_test-strategy.md` (measurement-scope.md defines every figure). Tests: passed / failed from the runner. Coverage: `unit` row = unit tests only, `non-unit` row = integration, acceptance, architecture and end-to-end tests that run under the coverage tool, `all levels` row = one merged report. Mutation score on the `all levels` row, with scope, tool, killed of total and the tests run. Write "—" where a cell does not apply. Goal: high coverage and mutation score. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 80 / 0 | — | — | — | JUnit 6, Testcontainers |
| backend | integration | 9 / 0 | — | — | — | JUnit 6, JDK HttpServer stub |
| backend | architecture | 8 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 106 / 0 | 70.21 % | 90.28 % | — | JUnit 6, JaCoCo 0.8.12 (phase 5, 103 tests) |
| backend | non-unit | 97 / 0 | 88.09 % | 71.18 % | — | JaCoCo 0.8.12 |
| backend | all levels | 203 / 0 | 98.19 % | 97.92 % | 90.54 % (335 of 370; `si.konferenca.registration.*` minus entry point; full JUnit suite) | JaCoCo, PIT 1.30.0 |
| frontend | acceptance | 7 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 49 / 0 | 99.41 % | 96.69 % | — | Vitest, v8 |
| frontend | non-unit | 7 / 0 | 78.23 % | 67.76 % | — | Vitest, v8 |
| frontend | all levels | 56 / 0 | 100 % | 98.34 % | 92.58 % (412 of 445; `src/**/*.{ts,tsx}` minus `main.tsx`, `*.d.ts`; Vitest suite) | v8, Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 on the compose stack |

## Fix loops

<!-- one row per loop: finding, category, needs a human (yes/no), minutes, change. Goal: few, short, no human needed; note any loop that a template rule could have prevented. -->

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 (survivor in captcha-mode mapping) | test-gap | no | 16.9 | `FormConfigControllerTest`; a template rule could not have prevented it, the mutation report did its job |

## Commits

<!-- auto:commits -->
| Measure | Value |
|---|---|
| Commits | 63 (chore 3, build 8, docs 12, test 21, feat 17, refactor 1, fix 1) |
| Changed lines per commit: median / 90th percentile / largest | 134 / 397 / 837 |
| Files per commit (median) | 3 |
| Over the size guide / mixing concerns / in a burst | 9.5 % / 11.1 % / 28.6 % |
| Subject follows the format and the 72-character limit | 98.4 % |
| Active minutes between commits (median / longest gap) | 1.4 / 20.9 |
| Cost per commit (median) / per 100 changed lines | $0.39 / $0.32 |
<!-- /auto:commits -->

## Decisions and human interventions

<!-- counts of blocking and non-blocking decisions by category; one row per human intervention with cause and minutes. Goal: few blocking decisions; list every decision that a pre-approved answer in the inputs could have avoided. -->

15 decisions: 1 blocking (environment), 14 non-blocking (environment 3, dependency 3, security-finding 4, requirement 2, other 2).

| Intervention | Cause | Minutes |
|---|---|---|
| D-03 Docker Compose 5.6.0 instead of 5.5.1 | decision | 5.2 |

Avoidable with a pre-approved answer: D-03 (PA-05 covers patch versions only; a minor-version Compose row would have avoided the wait), D-04 (cloc tag reporting another version), D-05 (OSS Index analyser needing credentials), D-11 (a contract validator in `tech-stack.md`), D-12 (`@types/node` for the Playwright helpers), D-13 (lint rule set for Spring/records).

## Divergences

<!-- every departure from the rules, phases or stack, from `divergences[]`; "none" if none. Goal: none. -->

| Phase | Rule | Divergence |
|---|---|---|
| 2 | Commits (size) | spec and contracts as one commit (phases.md unit) with 863 lines |
| 4 | Commits (subject) | `bcbec0a` subject 85 characters |
| 4 | Commits (size) | frontend UI commit `8e52d5f` 545 lines |
| 5 | Commits (size) | test commits `dec95f4` (407) and `569406c` (440) |
| 6 | gate: clean tree | frontend Vitest coverage / Stryker configuration committed after the phase 5 gate (`5dd4ca8`) |

## What held the run back

<!-- at most three items, each one sentence with its evidence: the largest causes of agent time and of cost (phase, tool class, waits, repeated fixes). -->

1. Mutation testing was the longest tool time: the backend PIT full run took about 15 minutes in phase 5 and again in phase 6 after a one-test fix (the rule requires the full re-measurement), because each mutant reruns Spring Boot acceptance tests with Testcontainers.
2. Phase 3 (35 min) was the longest phase: the frozen tests needed their own harness (Testcontainers, Mailpit API, an xlsx reader for Playwright) and two self-check corrections (Mailpit cannot search non-ASCII; AC-005-03 passed on a 401) before the freeze.
3. Shell quoting in heredocs broke several file writes and two harness probes (an interactive `python` and a malformed SMTP probe), costing retries; writing files with the editor tool avoided it from phase 4 on.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
| Item | Value |
|---|---|
| Cost (calculated from tokens and the prices in usage.md) | $37.14 ($32.45 without the cache rebuilds caused by waits) |
| Model calls / tool calls | 273 / 349 |
| Model time / tool time / agent time | 60 min / 78 min / 2 h 28 min |
| Session wall time | 11 h 15 min |
| Share of the cost at over 150k context | 92.1 % |
| Context per call (average / peak) | 355k / 639k |
| Approval prompts that needed a human | 1 (of 349 permission decisions; 0 rejected) |
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
