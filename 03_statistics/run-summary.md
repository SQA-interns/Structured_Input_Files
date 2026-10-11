# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Figures from `run-log.json`; no estimates. Cost, tokens and commit statistics come from the post-run session (`metrics.md` section 2) and are "pending" here.

## Run

| Item | Value |
|---|---|
| Run id | `kyuhi-single-agent-sdd-v004-opus-5-5-t1.4-20261011T0903Z-4af9` |
| Model / effort | claude-opus-5-5 / medium |
| Template version | 1.4 |
| Start | 2026-10-11T09:03:37Z, commit `d2f7e14` |
| End | written by `stat.mjs phase 7 end` (`end`, `finalCommit` in `run-log.json`) |

## Time (human waiting excluded)

Minutes from the clock tool's phase boundaries.

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|
| 0 Preflight and bootstrap | 6.8 | 0 | 6.8 |
| 1 Requirements | 1.5 | 0 | 1.5 |
| 2 Design | 4.8 | 0 | 4.8 |
| 3 Test design | 15.3 | 0 | 15.3 |
| 4 Build | 25.6 | 4.0 (D-12, frozen test harness) | 21.6 |
| 5 Unit tests | 21.9 | 0 | 21.9 |
| 6 Verify | 19.3 | 0 | 19.3 |
| 7 Release | closed by `phase 7 end` | 0 | see `run-log.json` |
| Total phases 0–6 | 95.2 | 4.0 | 91.2 |

No wait over one hour.

## Outcome

- Tests: first complete run (phase 5 start) 102 passed, 0 failed (100 acceptance, 2 e2e). Final run 218 passed, 0 failed: unit 107, integration 3, architecture 6, acceptance 100, e2e 2.
- Acceptance criteria: 35; frozen tests: 102. One frozen-test change, approved by the human: D-12/D-13 (harness property registration, no assertion changed).
- Findings as found: Critical 0, High 1 (pre-approved false positive), Medium 7, Low 4. Open at the end: Critical 0, High 0, Medium 0.

## Quality measures

| Component | Production / test lines (cloc) | Line coverage unit / nonUnit / all | Branch coverage unit / nonUnit / all | Mutation score (scope, tool, mutants) | Duplication |
|---|---|---|---|---|---|
| backend | 1,922 / 3,022 | 73.2 / 91.6 / 99.0 % | 86.2 / 75.1 / 93.1 % | 100 % (all production code except the entry point; PIT 1.30.0 with the full suite; 320/320) — phase 5: 91.9 % (282/307) | CPD: none |
| frontend | 533 / 757 | 87.9 / 84.3 / 90.0 % | 81.0 / 77.0 / 96.0 % | 85.9 % (all `src` production code; Stryker 10.0.0 with the Vitest suite, e2e excluded per D-15; 304/354) — phase 5: 77.7 % (275/354) | jscpd 1.45 % |

Complexity: not measured (no complexity tool in `tech-stack.md`).

## Fix loops

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|
| 1 | F-01 | security-config | no | 2 | fixed host in the production HTTPS redirect example |
| 2 | F-09 | test-gap | no | 0.3 | frontend tests for the unavailable form and the general error |

## Commits

Pending (post-run `commitSummary`).

## Decisions and human interventions

| Category | Blocking | Non-blocking |
|---|---|---|
| environment | 0 | 1 (D-01) |
| dependency | 0 | 2 (D-02, D-11) |
| security-finding | 0 | 3 (D-03, D-04, D-14) |
| requirement | 0 | 6 (D-05 … D-10, pending review) |
| frozen-test | 2 (D-12 and its follow-up D-13) | 0 |
| other | 0 | 1 (D-15) |

| Intervention | Cause | Minutes |
|---|---|---|
| D-12: frozen acceptance harness ignores per-class property overrides | decision | 4.0 |

Decisions an input could have avoided: D-11 (`@types/node` missing from `tech-stack.md`); D-12 would have been avoided by running a per-class override probe in the phase 3 self-check.

## Divergences

| Phase | Rule | Divergence |
|---|---|---|
| 0 | working-rules.md, commits | commit `f6d37e6` (backend skeleton) also contains two docs files (staged by `git add --renormalize`) |
| 4 | working-rules.md, commit size | commit `1a73cc9` (US-003) has 567 changed lines without a stated reason |

## What held the run back

1. Phase 4 build was the largest phase (25.6 min wall), including the 4-minute wait for D-12, a frozen-harness defect that the phase 3 self-check did not detect (subclass `@DynamicPropertySource` overrides lose to the base class).
2. Phase 5 (21.9 min) and phase 6 (19.3 min) were dominated by full backend suites with Testcontainers (about 1.5 min each) run per coverage level, plus one PIT run per phase (about 5 min each).
3. Shell quoting and Windows path issues (heredocs with apostrophes, Git Bash path rewriting for Docker, Windows `curl` and `MAX_PATH` on the clean clone) cost several repeated commands.

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
