# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

## Run

| Item | Value |
|---|---|
| Run id | `kyuhi-single-agent-sdd-v003-opus-5-5-t1.3-20261012T1355Z-5267` |
| Label | kyuhi_single-agent_sdd_v003 |
| Model, effort | claude-opus-5-5, medium |
| Template version | 1.3 |
| Start | 2026-10-12T13:55:04Z, commit `e74fb75` |
| End | written by `stat.mjs phase 7 end` (final commit in `run-log.json`) |

## Time (human waiting excluded)

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 preflight-and-bootstrap | 110 | 99 D-01 (decision) | 11 |
| 1 requirements | 2 | 0 — | 2 |
| 2 design | 5 | 0 — | 5 |
| 3 test-design | 19 | 0 — | 19 |
| 4 build | 164 | 0 — | 164 (includes the unrecorded usage-limit stop) |
| 5 unit-tests | 8 | 0 — | 8 |
| 6 verify | 59 | 27 D-25 (decision) | 32 |
| 7 release | 5 | 0 — | 5 (until this summary) |
| total | 372 | 126 | 246 |

Minutes from `stat.mjs` timestamps. Waits over one hour: D-01 in phase 0 (99 min: host versions, vulnerable dev tooling and a CVE false positive). Phase 4 also contains a stop for the plan's usage limit that could not be recorded live (divergence below); the post-run step measures that idle gap from the transcript.

## Outcome

| Item | Value |
|---|---|
| Acceptance criteria | 49, all with tests; 69 frozen tests; frozen-test changes: 0 |
| First full run (phase 5, all levels) | 165 passed, 2 failed (both non-frozen test defects) |
| Final run (clean clone) | 183 passed, 0 failed |
| Findings as found | Critical 0, High 1, Medium 5, Low 6 |
| Open at end | Critical 0, High 0, Medium 3 (F-03 awaiting a `.env` change by the human, F-05, F-08 accepted) |

The High (F-06, Semgrep on HTTP Basic) was lowered to Low by the human (D-25, D-26).

## Quality measures

| Component | Production / test LOC | Line coverage unit / nonUnit / all | Branch coverage unit / nonUnit / all | Mutation | Duplication |
|---|---|---|---|---|---|
| backend | 1,900 / 3,023 | 71.2 / 90.1 / 95.2 % (JaCoCo) | 79.0 / 68.9 / 84.3 % | PIT 1.30.0: score 72.6 % (252/347), covered 96.2 % (252/262); all production code minus entry point; unit + architecture tests (D-23) | 0 (PMD CPD) |
| frontend | 581 / 968 | 66.9 / 75.5 / 97.8 % (Vitest v8; e2e not instrumented) | 67.8 / 65.6 / 90.0 % | Stryker 10.0.0 command runner (D-24): 88.2 % (314/356, 169 of them timeouts; killed only 40.7 %); all production code minus `main.tsx`, `vite-env.d.ts`; full Vitest suite | 0 % (jscpd) |

LOC by `aldanial/cloc:2.10` (reports 1.98). Backend unit coverage measured before F-01 added six unit tests. Complexity was not measured: no complexity tool is listed in `tech-stack.md`.

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 | test-gap | no | 2 | unit tests for surviving-mutant gaps |
| 2 | F-02 | security-config | no | 1 | redacted generated skeleton passwords in phase 3 logs |
| 3 | F-04 | security-config | no | 5 | nginx sends a fixed Host header |
| 4 | F-03 | process | yes | 2 | removed leaked log line; header-based credential check |
| 5 | F-12 | test-gap | no | 9 | frontend tests for email anchor and type-switch filtering |

Loop 4 (F-03) was caused by an agent command; a template rule "never pass credentials to curl with `-u`; build the header in the shell" would have prevented it.

## Commits

pending (filled by the post-run step from `git log`). Known from phase 6: six commits exceed the size guide (F-10, `out/logs/phase6-commit-size.log`).

## Decisions and human interventions

28 decisions: 9 blocking (environment 2, dependency 2, security finding 4, requirement 1), 19 non-blocking (requirement 10, dependency 4, other 3, environment 2). D-28 (after the run ended) approves all 16 decisions that were pending review; none is pending.

| Intervention | Phase | Cause | Minutes |
|---|---|---|---|
| D-01, D-04, D-05 (answered together: D-07, D-08, D-09) | 0 | decision | 99 |
| D-25 (answered: D-26) | 6 | decision | 27 |
| plan usage limit, session resumed by the human | 4 | session | not recorded live |
| approval of the 16 pending decisions (D-28) and `.env` line 2 fixed | after phase 7 | decision | not timed (run already ended) |

Avoidable with pre-approved answers in the inputs: D-01 (host JDK/Docker/Compose versions), D-04 (vitest/jscpd fixed versions; the run guide already names them), D-05 (CVE-2025-7962 false positive) and D-25 (Semgrep Basic auth) all have standard answers in `Run_Guide.md`; placing them in `tech-stack.md` or a pre-approved decisions file would have removed both waits.

## Divergences

- Phase 3 (phases.md phase 3 manifest): 03_acceptance-manifest.sha256 also lists out/logs/phase3-e2e-run.log (path contains 'e2e'); the log is frozen and harmless, no test affected
- Phase 4 (working-rules.md Statistics (wait start/end)): session stopped by the plan usage limit during phase 4 and resumed by the human; the stop could not be recorded with stat.mjs wait start, so the gap appears only as idle time in the transcript
- Phase 6 (working-rules.md Secrets): a curl password prompt echoed part of the ORGANIZER_USERNAME value into the conversation and an uncommitted log during the runtime demonstration; the log line was removed before commit (finding F-03); the transcript still holds the fragment
- Phase 6 (working-rules.md Commits (size guide)): 6 commits exceed 15 files or 400 changed lines without a stated reason except 8aa888c: d26873f (640, mostly the generated mvnw script), b6fb493 (16 files, 597), 98eac54 (524), de033ab (625), 97dc216 (436); history is not rewritten (finding F-10)
- Phase 6 (D-16): Redocly CLI, run once via npx in phase 2, was not scanned in phase 6 as D-16 stated (recorded in D-27)

## What held the run back

1. Waiting for the human: 126 recorded minutes (phase 0: 99, phase 6: 27), all on decisions with standard answers in `Run_Guide.md`.
2. Phase 4 build was the longest phase (wall time inflated by the usage-limit stop); repeated full backend suites with Testcontainers (about 1–3 min each) dominated tool time.
3. Tooling incompatibility after D-08 (Vitest 5 vs Stryker's Vitest runner 10.0.0) cost two invalid Stryker runs (about 25 min) before the command runner (D-24).

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
