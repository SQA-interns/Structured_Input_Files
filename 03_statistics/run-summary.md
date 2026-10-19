# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. The sections **Run, Time, Commits and Cost and usage** sit between `<!-- auto:... -->` markers and are written by `tools/build-report.mjs` after the run (the agent's last commit comes before the report exists): do not fill them in and do not remove the markers.

## Run

<!-- auto:run -->
<!-- /auto:run -->

## Time (human waiting excluded)

<!-- auto:time -->
<!-- /auto:time -->

## Outcome

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | 89: backend acceptance 77, frontend acceptance 7, end-to-end 5; all failed on assertions against the skeleton (no build or harness error); no frozen test changed |
| TDD baseline (phase 5, before any added test) | backend 232 tests, line 96.34 % / branch 87.02 %; frontend 56 tests, line 97.85 % / branch 93.58 % |
| TDD order (phase 6, `trace-ac.mjs`) | 35 of 35 ACs test-first; none without a test or a commit |
| First complete run (phase 5, before any fix) | 293 passed, 0 failed |
| Final run (phase 6) | 349 passed, 0 failed |
| Coverage, all tests | backend line 97.64 % / branch 91.22 % (JaCoCo); frontend line 100 % / branch 98.71 % (Vitest v8) |
| Mutation score | backend raw 99.10 %, adjusted 100 % (PIT 1.30.0; 331 killed, 3 equivalent, 334 total; all production code without the entry point); frontend raw 98.10 %, adjusted 100 % (Stryker 10.0.0; 362 killed, 7 equivalent, 369 total; all production source without bootstrap and test code) |
| Quality gate | met for both components (line >= 95 %, mutation >= 90 %); 1 survivor round each; tests at the first mutation run / at the end: backend 232 / 264, frontend 56 / 80; no shortfall |
| Findings as found | Critical 0, High 1 (CVE-2025-7962, pre-approved PA-01), Medium 3, Low 3 |
| Findings after triage | open Critical or High: 0; no finding lowered by the human (the High was lowered by pre-approval PA-01, D-05) |
| Fix loops | 0 |
| Code (cloc) | backend 1982 production / 4268 test lines; frontend 472 production / 1848 test lines |
| Duplication | backend 0 % (PMD CPD); frontend 0 % (jscpd 5.4.0) |

## Test measures

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | 77 / 0 | — | — | — | JUnit 6, MockMvc, Testcontainers |
| backend | integration | 24 / 0 | — | — | — | JUnit 6, Testcontainers |
| backend | architecture | 6 / 0 | — | — | — | ArchUnit 1.3.2 |
| backend | unit | 157 / 0 | 86.66 % | 89.69 % | — | JUnit 6, JaCoCo 0.8.12 |
| backend | non-unit | 107 / 0 | 88.43 % | 69.08 % | — | JaCoCo 0.8.12 |
| backend | all levels | 264 / 0 | 97.64 % | 91.22 % | adjusted 100 %, raw 99.10 %, 331 of 334 killed, all production code, full suite (264 tests) | PIT 1.30.0 |
| frontend | acceptance | 7 / 0 | — | — | — | Vitest 4.1.11, Testing Library |
| frontend | unit | 73 / 0 | 100 % | 98.71 % | — | Vitest 4.1.11, coverage-v8 |
| frontend | non-unit | 7 / 0 | 79.28 % | 67.94 % | — | coverage-v8 |
| frontend | all levels | 80 / 0 | 100 % | 98.71 % | adjusted 100 %, raw 98.10 %, 362 of 369 killed, all production source, full suite (80 tests) | Stryker 10.0.0 |
| frontend + backend | end-to-end | 5 / 0 | — | — | — | Playwright 1.63.0 against docker compose |

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| none | – | – | – | – | no Critical or High finding; Medium and Low findings were accepted with reasons (`02_output/docs/06_verification-report.md`) |

## Commits

<!-- auto:commits -->
<!-- /auto:commits -->

## Decisions and human interventions

| Type | Category | Count | Decisions |
|---|---|---|---|
| blocking | environment | 1 | D-01 Docker engine not running |
| non-blocking | security-finding | 3 | D-02 (PA-03), D-05 (PA-01, PA-02), D-11 SpotBugs patterns |
| non-blocking | environment | 1 | D-03 cloc image reports 1.98 |
| non-blocking | dependency | 2 | D-04 OSS Index analyser disabled, D-10 `@types/node` |
| non-blocking | requirement | 3 | D-06 stray answer fragment, D-08 English UI (pending review), D-09 status codes and limits (pending review) |
| non-blocking | other | 1 | D-07 timestamp correction |

| Intervention | Cause | Minutes |
|---|---|---|
| Docker Desktop started by the human (D-01) | environment | 0.7 |

Decisions an input could have avoided: D-01 (a run-guide step "start Docker before the run"), D-04 (an `options` entry for Dependency-Check disabling OSS Index in `tech-stack.md`), D-10 (`@types/node` in `tech-stack.md`), D-09 (numbers for SR-03 and a status for AC-008-04).

## Divergences

| Phase | Rule | Divergence |
|---|---|---|
| 2 | working-rules.md, commit size | the specification commit has 521 changed lines (one commit as phases.md asks) |
| 3 | working-rules.md, subject length | freeze commit `0b8e69e` subject longer than 72 characters |
| 4 | working-rules.md, subject length | commit `575246e` subject longer than 72 characters |
| 4 | phases.md, unit test per AC | AC-002-05 and AC-002-06 rely on the type-independent unit tests of AC-001-06/07 and the frozen student acceptance tests |
| 4 | phases.md, commit units | AC ids added to backend unit test classes after their cycles (`515f7d0`, text only) so that `trace-ac.mjs` finds them |

## What held the run back

1. Mutation testing in phase 5: the full PIT run over the Spring and Testcontainers suite took the longest single wait, and its survivors were mostly `@Bean` methods hidden by the cached Spring test context, which needed extra configuration and wiring tests (`02_output/logs/phase5-pit-full.log`).
2. The TDD build of phase 4 (12 cycles, 19 red/green pairs) was the longest phase, because each backend cycle started Spring and Testcontainers for its red and green runs (`03_statistics/run-log.json`, phases).
3. Shell quoting on Windows: several heredoc and `node -e` commands failed to parse and had to be redone with file writes, and Git Bash path conversion broke container paths in the runtime demonstration until `MSYS_NO_PATHCONV` was set.

## Cost and usage (calculated from the transcript)

<!-- auto:usage -->
<!-- /auto:usage -->

Everything is calculated from the session transcript, the sub-agent transcripts and git by `tools/build-report.mjs`; the complete report is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`.
