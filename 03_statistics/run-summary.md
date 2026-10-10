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

<!-- tests: first and final run (all levels), findings as found and open at the end (Critical, High, Medium, Low), frozen tests and changes to them. Goal: final run all green, no open Critical or High. -->

## Quality measures

<!-- per component: production and test lines, line and branch coverage per level, mutation score WITH its scope (tool, packages, number of mutants), duplication, complexity. Goal: high coverage and mutation score; always say what the number covers. -->

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
