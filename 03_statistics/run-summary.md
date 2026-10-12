# Run summary

> Written in: phase 7 · Source: `run-log.json` · Agent: writes

Write the tables from `run-log.json`; no estimates. The summary answers three questions: what did the run cost in agent time and money, what did it deliver, and what held it back. Cost and token rows are filled by the post-run session (`metrics.md` section 2); until then write "pending".

## Run

<!-- run id, model, effort, template version, start and end commit -->

## Time (human waiting excluded)

<!-- one row per phase: wall minutes, minutes waiting for the human with the decision or cause, agent minutes. Total row. Goal: low agent time. State every wait over one hour and what caused it. -->

| Phase | Wall | Waiting for the human | Agent time |
|---|---|---|---|

## Outcome

<!-- One row per measure, values from `run-log.json`; keep the rows and their order. Goal: final run all green, no open Critical or High; always say what a coverage or mutation figure covers. -->

| Measure | Value |
|---|---|
| Frozen tests (phase 3) | number, split by component and level; all failed for behavioural reasons before the build (and any change to a frozen test with its decision id) |
| First complete run (phase 5, before any fix) | passed, failed |
| Final run (phase 6) | passed, failed |
| Coverage, all tests | per component: line % / branch % |
| Mutation score | per component: raw and adjusted score, tool, number of mutants (killed, equivalent, total), scope |
| Quality gate | met or not per component and threshold (line, mutation); survivor rounds used; tests at the first mutation run and at the end; any shortfall with its decision id |
| Findings as found | Critical, High, Medium, Low |
| Findings after triage | open Critical or High (goal 0); every finding lowered by the human, with the decision id |
| Fix loops | number, each with its finding and minutes |
| Code (cloc) | per component: production / test lines |
| Duplication | per component: percent and tool |

## Test measures

<!-- One row per component and level, from `docs/03_test-strategy.md` (measurement-scope.md defines every figure). Tests: passed / failed from the runner. Coverage: `unit` row = unit tests only, `non-unit` row = integration, acceptance, architecture and end-to-end tests that run under the coverage tool, `all levels` row = one merged report. Mutation score on the `all levels` row, with scope, tool, killed of total and the tests run. Write "—" where a cell does not apply. Goal: high coverage and mutation score. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (scope) | Tools |
|---|---|---|---|---|---|---|
| backend | acceptance | | — | — | — | |
| backend | integration | | — | — | — | |
| backend | architecture | | — | — | — | |
| backend | unit | | | | — | |
| backend | non-unit | | | | — | |
| backend | all levels | | | | score, killed of total, scope, tests run | |
| frontend | acceptance | | — | — | — | |
| frontend | unit | | | | — | |
| frontend | non-unit | | | | — | |
| frontend | all levels | | | | score, killed of total, scope, tests run | |
| frontend + backend | end-to-end | | — | — | — | |

## Fix loops

<!-- one row per loop: finding, category, needs a human (yes/no), minutes, change. Goal: few, short, no human needed; note any loop that a template rule could have prevented. -->

| Loop | Finding | Category | Needs human | Minutes | Change |
|---|---|---|---|---|---|

## Commits

<!-- from `commitSummary` once the post-run step has run, otherwise "pending": count, median and largest size, shares over the size guide, mixed concerns, bursts, cost and minutes between commits. Goal: small finished steps, none over the guide, none mixed, no bursts. -->

## Decisions and human interventions

<!-- counts of blocking and non-blocking decisions by category; one row per human intervention with cause and minutes. Goal: few blocking decisions; list every decision that a pre-approved answer in the inputs could have avoided. -->

## Divergences

<!-- every departure from the rules, phases or stack, from `divergences[]`; "none" if none. Goal: none. -->

## What held the run back

<!-- at most three items, each one sentence with its evidence: the largest causes of agent time and of cost (phase, tool class, waits, repeated fixes). -->

## Not measured by the agent

Tokens, cost, model and tool time, context size, cache rebuilds, commits and the environment are written into `run-log.json` by `tools/build-report.mjs` after the run; everything it generates is in `03_statistics/results/` and the transcripts are backed up in `03_statistics/data/`. API time, approval prompts and the panel cost are added by a human in `usage.md`.
