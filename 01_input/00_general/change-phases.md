# Change phases (a signed change request on a finished system)

> Owner: Team lead · Read in: every phase of a change run · Agent: read-only

Adds to `general/phases.md`, which stays in force for everything not stated here; where they differ, this file wins. A change run builds one signed change request (CR) on a finished baseline, under the same gates, quality gate, done criteria, security baseline and commit rules as the baseline run. Nothing is relaxed: the scope is the CR plus a regression check of everything else. The phase numbers are those of `phases.md`, so `stat.mjs` and the reports are unchanged.

## Paths and scope

- `cr/` = `04_changes/CR-nnn_<slug>/`; the CR's inputs (`change.md`, `acceptance-criteria.md`, optional `tech-stack.md`) are read-only. `base` = the commit named under "Baseline" in `cr/change.md`.
- In a change run `docs/` means `cr/docs/`. The baseline's documents in `02_output/docs/` are read, not rewritten, except the living documents: `02_specification.md`, `02_contracts/`, `out/README.md` and the component READMEs, which are updated in place so that they always describe the whole system.
- Effective requirements = the `project/` files plus the CR. The CR wins only for the ids in its "Baseline items changed" table and for the items it adds. Everything else in the baseline holds, including every id under "Must not change".
- Precedence in a change run: `AGENTS.md` > `general/working-rules.md` > this file > `general/phases.md` > other `general/` files > `project/` files.
- Resume from `docs/00_progress.md`; the baseline's progress file says "done" and is not touched.
- New decision ids continue after the highest `D-nn` of `02_output/docs/decisions-log.md`.

## Phases

| # | Phase | In a change run | Gate (replaces the gate in `phases.md`) |
|---|---|---|---|
| 0 | Preflight & baseline check | Run `general/skills/preflight` as written (tools, services, secrets, dependency scan) against the baseline `tech-stack.md` plus `cr/tech-stack.md` if present; no skeletons. Run `stat.mjs phase 0 start` and `stat.mjs wait end` (the gap since the interview), and add this session's transcript file to `transcripts` in `run-log.json` (the newest `.jsonl` in `~/.claude/projects/<working directory, every non-alphanumeric character replaced by "-">/`). Repeat the baseline check of `general/skills/change-interview` step 2, run `lint-change.mjs cr --signed`, and run the full suite once on the unchanged baseline: record it as `change.baselineTestRun`. Write one non-blocking decision for each MODIFIED or REMOVED item, category `other`, resolution "approved by the signed CR", `Human response` = the CR's `Approved` time. Write `docs/00_input-manifest.sha256`: the files of preflight step 8 plus the CR's input files. | Every preflight check passes; the baseline check passes; the baseline suite has 0 failed; the CR lint is clean; manifest written. Any failure is a blocking decision and nothing is repaired. |
| 1 | Requirements | `docs/01_requirements-check.md`: for each CR story, its criteria (gaps only, nothing copied); and every baseline item that a CR item touches or contradicts without being listed in "Baseline items changed". | Every CR story has at least one AC; each undeclared conflict or gap is a blocking decision (the human decides whether a new request replaces the CR). |
| 2 | Design | `docs/02_impact.md` (format below) before any production code; `docs/02_specification.md` as an index, per CR AC, SR and NFR the section of the living specification that realises it; update the living specification and contracts in place. A change to persistent data keeps existing data (ES-08). | Every CR AC, SR and NFR maps to the specification; contracts validate with a parser; the impact table exists. |
| 3 | Test design | `general/skills/write-acceptance-tests` with `cr/acceptance-criteria.md` as input: tests for every ADDED and MODIFIED criterion, exactly one end-to-end test per CR scenario and no other (a MODIFIED scenario updates its existing test). For a MODIFIED or REMOVED id, change or delete only the test cases named by that id in the baseline's frozen files, and name the id in the commit. `docs/03_acceptance-manifest.sha256` lists the CR's new test files and the current state of every changed baseline test file. | Every ADDED and MODIFIED AC has a test; the only failing tests of the full suite are those of ADDED and MODIFIED items, each for a behavioural reason; every other baseline test passes; the baseline manifest differs only as the supersession rule allows; the CR manifest is committed last, as its own freeze commit. |
| 4 | Build | As `phases.md`. One commit per layer of each CR story, and per baseline story that a MODIFIED item touches; subjects name the CR's `US` and `AC` ids. A file outside `docs/02_impact.md` that must change gets a row under "Found during build" with the reason; the predictions are never edited. | All frozen tests, baseline and CR, pass; format, lint and type checks are clean; at least one commit per CR story; no behaviour under "Must not change" differs. |
| 5 | Unit tests | As `phases.md`. The first full run covers the whole suite; also record `firstTestRun.failedBaseline`, the failures in tests that existed at `base`. The quality gate applies to every component whose production code or tests changed since `base` (`git diff --stat base`), with the same thresholds, scope and tests as the baseline; a component not changed keeps the baseline's recorded result. | Gate met for each changed component, or the shortfall recorded after the third round; full suite passes. |
| 6 | Verify | `general/skills/verify-release`, with: step 2 recomputes the CR manifests in `docs/` and also the baseline manifests in `02_output/docs/` (under the supersession rule, ignoring the `run-config.md` line); step 4 the CR's flows and again the project's runtime demonstrations (`DoD-Pnn`); step 9 `trace-ac.mjs cr/acceptance-criteria.md`, where a REMOVED id has no test by design. Every DoD item needs evidence for the system after the change; a baseline result is reused only where its component and tests did not change, and the report says so. | As `phases.md`, plus the supersession rule and the lint (`--signed`) hold. |
| 7 | Release | READMEs updated where affected and followed from a clean checkout; `docs/release-notes.md` lists the manual tests of the CR, data migrations and configuration changes; `stat.mjs phase 7 end`. | As `phases.md`. |

## Impact table (`docs/02_impact.md`)

One table of the production code and test files the change is expected to touch (a folder row covers the files under it), then a list of the baseline test files that MODIFIED or REMOVED ids require changing, then an empty section "Found during build". The post-run report compares the table with `git diff --name-status base` and computes precision, recall and the files outside it.

| Path | Action (add / modify / delete) | Because (ids) |
|---|---|---|

## Supersession rule (phase 6)

A file in `02_output/docs/03_acceptance-manifest.sha256` either still matches, or is listed in `docs/03_acceptance-manifest.sha256` and its `git diff base -- <file>` touches only test cases named by a MODIFIED or REMOVED id of the CR. A deleted baseline test needs a REMOVED id. Any other difference is a Critical finding.

## Constraints

- Do not edit `cr/change.md`, `cr/acceptance-criteria.md`, `cr/tech-stack.md` or `cr/input.sha256`. A needed change is a blocking decision; the human decides whether to start a new request.
- Do not change behaviour that is not in the CR, and do not refactor outside the impact table without a decision.
- Do not lower a threshold or narrow a scope to meet the quality gate.
- Record only what a script cannot see: the baseline suite result, the failures of existing tests in the first full run, and the changed-code mutation figure. Everything else about the change is computed after the run (`03_statistics/metrics.md`, "Change run").
