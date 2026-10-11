# Usage (human)

> Owner: Experiment lead · Filled: prices before the post-run session, `Approval prompts` after the run; the rest is filled by `tools/build-report.mjs` · Agent: reads prices only

## Prices used for `usage.costUsd`

`tools/build-report.mjs` reads the price rows below, so keep the table shape: one row per model id used in the run, five prices per row, each with a `$`. A model without a row is left unpriced and listed in `usage.unpricedModels`.

Source and date: Anthropic first-party API list prices, Claude Code `claude-api` skill model table (cached 2026-09-25); cache-write multipliers 1.25× (5 min) and 2× (1 h) of the input price. Claude Code writes 1-hour cache entries. Check the prices against https://www.anthropic.com/pricing before each round and change the date if they changed; add a row for any other model used in the run (for example the small helper model, which costs under a cent and is otherwise left unpriced).

| Model | Input / MTok | Output / MTok | Cache read / MTok | Cache write 5 min / MTok | Cache write 1 h / MTok |
|---|---|---|---|---|---|
| claude-opus-5-5 | $4.00 | $20.00 | $0.20 | $5.00 | $8.00 |

## From the harness usage panel

Run `/cost` once at the end of the session, before the post-run step: the plan limits then end up in the transcript. Empty cells are filled automatically from the transcript (its `cost-state` record is what the panel shows); only `Approval prompts` has no source in a transcript. Goal: cross-check `usage.costUsd`; a difference above 1.5 % means a model, a sub-agent or a price is missing from the calculation.

| Item | Value |
|---|---|
| API time | 50m |
| Session wall time | 93m |
| Approval prompts | |
| Cost shown | $26.37 |
| Tokens shown per model (input, output, cache read, cache write) | Opus 5.5: 392 in / 2.7k out / 69.9M cache read / 551.8k cache write |
| Share of usage at over 150k context | |
| Plan limits used (session, week) | 73%, 24% |
| Difference to `usage.costUsd` | +$0.42 (1.6 %), check: sub-agents, resumed sessions, activity after the run, or a missing price |

Raw panel text (filled from the session when `/cost` was run in it):

```text
Claude Code usage report (2026-10-11T10:52:20.289Z)

Plan limits:
- session-0: 73% (resets 2026-10-11T13:29:59.941219+00:00)
- weekly_all-1: 24% (resets 2026-10-17T02:59:59.941242+00:00)

Session:
- Opus 5.5: 392 in / 2.7k out / 69.9M cache read / 551.8k cache write
- Cost: $26.37 | API 50m | Wall 93m

Local activity: 572 requests (24h) | 2908 (7d)
```
