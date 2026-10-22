---
name: change-interview
description: Interview the human about a feature to add, change or remove in the finished system, and write the signed-off change request (CR) folder in 04_changes/ from the templates.
---

> Owner: Team lead · Read in: change interview · Agent: read-only

- Reads: the baseline inputs (`project/01_requirements/*`, `project/02_design/*` only when the request touches them), the baseline's `02_output/docs/00_progress.md`, `decisions-log.md` and both manifests, `03_statistics/run-log.json`, every CR folder already in `04_changes/`, this skill's `templates/` and `example/`
- Writes: only `04_changes/CR-nnn_<slug>/` (`change.md`, `acceptance-criteria.md`, `tech-stack.md` if a dependency is added, `interview-log.md`, `input.sha256`)
- Does not write: code, tests, specifications, any baseline file, anything in `general/`

## Procedure

1. **Resume or start.** A CR folder without `input.sha256` is a draft: resume it from its `interview-log.md`. Otherwise take the next free `CR-nnn` and a short kebab-case slug from the human's request.
2. **Check the baseline.** Reads only, seconds, with explicit paths because `docs/` means something else in a change run. The baseline commit is HEAD now, and the commit named in `change.md` when this check is repeated in the build. At the first failure stop, tell the human what is wrong, and repair nothing:
   - the working tree is clean, apart from an untracked `.env` and `03_statistics/`;
   - `02_output/docs/00_progress.md` states that phase 7 is complete;
   - `03_statistics/run-log.json` as committed at the baseline commit (`git show <commit>:03_statistics/run-log.json`) has `end` set, `finalTestRun.failed` = 0, `codeMetrics.gate.met` true, and no open Critical or High in `openAtEnd`;
   - every `D-nn` in `02_output/docs/decisions-log.md` has a resolution (pending review counts);
   - `02_output/docs/00_input-manifest.sha256` and `03_acceptance-manifest.sha256` recompute and match, as in `general/skills/verify-release` step 2, except the line of `run-config.md`, which the human changes for every run (the new label is committed as `run setup <label>`);
   - the `Run label` in `run-config.md` differs from the baseline's `runLabel`.

   The test suite is not run here: the first step of the build session runs it. Record the baseline commit and branch in `change.md` and the result as row 0 of the log.
3. **Statistics.** Run `node 03_statistics/tools/stat.mjs init --force`: it replaces the baseline's `run-log.json`, which stays in git at the baseline commit. Around every round of questions run `stat.mjs wait start decision "round <n>"` and `stat.mjs wait end`.
4. **Learn the shape and the baseline.** Read `example/` once; copy its shape, never its content. Then read the baseline files the request needs, no more.
5. **Interview in rounds.**
   - A round is up to four questions whose prerequisites are settled. Number and title each one and give your recommended answer, worded so that "yes" accepts it. Use the question prompt for choices, plain conversation for stories and criteria.
   - The template fields are the question list. Go in this order: type, intent, stories, rules and design, baseline items changed, criteria, end-to-end scenarios, must-not-change, open questions.
   - Look up facts, ask decisions. Anything the baseline files answer (status codes, error shapes, field lists, which criteria the change makes false) you look up and confirm in one line.
   - Always probe, even when the request seems to cover it: every output of the baseline where the new behaviour or data could appear (screens, API, storage, notifications, exports); limits and empty, duplicate and Unicode input; failure behaviour; security impact (personal data, authentication, public endpoints, new dependency); what is out of scope.
   - Never invent behaviour. An unanswered question stays an open question and blocks sign-off.
6. **Write as you go.** After every round append its rows to `interview-log.md` and update the draft. Copy a template only when a file is needed (`tech-stack.md` only when a dependency or tool is added or changed); a missing file means "no change". Delete a hint when its field is filled and an optional section when it stays unchanged. IDs continue after the highest of each kind across the baseline files and every CR folder in `04_changes/`. Every sentence must be checkable by a test, gate or script, or be a constraint the baseline does not already give; delete the rest.
7. **Close.**
   1. Run `node 01_input/00_general/skills/change-interview/lint-change.mjs 04_changes/CR-nnn_<slug>` and fix what it reports.
   2. Read the CR back in at most ten lines (intent, stories, number of criteria, baseline items changed, open-question answers) and ask for explicit approval. Changes after the read-back restart this step.
   3. On approval replace `Approved: pending` with `date -u +%FT%TZ`, write `input.sha256` for `change.md`, `acceptance-criteria.md` and `tech-stack.md` (if present) in the format and line-ending normalisation of `general/skills/preflight` step 8 (repository-relative paths), run the lint again with `--signed`, and commit the folder as one documents commit (`docs: add change request <title> (CR-nnn)`, at most 72 characters).
   4. Run `stat.mjs wait start session "between interview and build"` so that the gap until the build session is excluded from agent time, then stop. Tell the human to start the build in a new session with `Read AGENTS.md and implement CR-nnn`. Do not begin it.

## Constraints

- Do not change a baseline file or anything outside the CR folder. A CR changes the baseline only by listing the IDs in "Baseline items changed".
- A CR cannot change anything in `general/` (security baseline, engineering standards, done criteria, quality gate). If the request needs that, say so and stop.
- Do not design the implementation. Record a technical constraint only when the requester states it.
- Do not ask what the baseline already answers, and do not ask more than four questions per round.
- Do not read `.env`; see "Secrets" in `general/working-rules.md`.
