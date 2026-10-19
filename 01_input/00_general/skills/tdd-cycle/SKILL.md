---
name: tdd-cycle
description: Build the system by red-green-refactor cycles, one acceptance criterion or one range of criteria at a time, until the frozen acceptance and end-to-end tests pass (phase 4).
---

> Owner: QA lead · Read in: phase 4 · Agent: read-only

- Reads: `docs/02_specification.md`, `docs/02_contracts/`, `docs/03_test-strategy.md`, `docs/03_acceptance-manifest.sha256`, `project/01_requirements/scope.md` (priorities), `general/quality/test-strategy.md`, `general/phases.md` ("TDD cycle", "Constraints")
- Writes: `docs/04_cycle-plan.md`, unit and integration tests, source code, red and green logs in `out/logs/tdd/` (through `tdd-step.mjs`)
- Tool: `node 01_input/00_general/skills/tdd-cycle/tdd-step.mjs <red|green|refactor> <cycle> "<subject>" [--body "<text>"] -- <test command>`, run from the repository root. In one call it checks the subject, runs the test command, writes the log, checks the outcome the step needs (red: fails on an assertion or a stub, not on a compile or harness error; green and refactor: pass, no test file changed), checks the commit size and commits everything except `03_statistics/`. It prints one line. On "refused" nothing is committed: fix the cause and run it again.

## Procedure

1. **Plan once.** Write `docs/04_cycle-plan.md`: one row per cycle in priority order (storage before notifications, participant flow before organizer features), with its cycle id (`C01`, `C02`, …; `C07a`, `C07b` when a cycle is split by layer), AC or AC range, component, the test classes you will add, the frozen tests the cycle should turn green and the exact test command for them. Group ACs only when they share one code path. Commit it alone (`docs:`).
2. **Red.** Write the unit or integration tests for the cycle's behaviour, with the AC id or range in each test name, and only the stubs they need to compile (signatures that report "not implemented"). Then run `tdd-step.mjs red <cycle> "test: <summary> (<AC range>)" -- <command for the touched test classes>`.
3. **Green.** Write the least production code that makes the new tests and the cycle's frozen tests pass; change no test. Run `tdd-step.mjs green <cycle> "feat: <summary> (<AC range>)" -- <command for the touched test classes and the cycle's frozen tests>`. If it refuses for size, split the cycle by layer into several red → green pairs with their own cycle ids.
4. **Refactor** only when the code needs it: change code, not tests, then `tdd-step.mjs refactor <cycle> "refactor: <summary> (<AC range>)" -- <same command>`.
5. **End of a user story:** run the full suite and the check commands (ES-05) once; fix anything that fails in a new red → green pair; update `docs/00_progress.md` (last cycle done, next cycle) and commit it alone (`docs:`).
6. **Gate:** when every cycle is done, run the full suite once more and check the phase 4 gate in `general/phases.md`.

## Constraints

- Do not commit a cycle step with `git commit` yourself; only `tdd-step.mjs` commits red, green and refactor steps.
- Do not pipe the test command or append `|| true`: the script needs its exit code and output.
- Do not read a whole log; the one-line result and, on a refusal, the end of the log are enough.
- Keep the subject's summary short (about 40 characters): the subject must fit 72 characters with its IDs; detail goes in `--body`.

A frozen test that cannot pass with a correct implementation is a blocking decision (`general/working-rules.md`); do not touch the test.
