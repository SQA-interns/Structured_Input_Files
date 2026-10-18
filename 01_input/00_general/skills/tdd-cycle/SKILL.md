---
name: tdd-cycle
description: Build the system by red-green-refactor cycles, one acceptance criterion or one range of criteria at a time, until the frozen acceptance and end-to-end tests pass (phase 4).
---

> Owner: QA lead · Read in: phase 4 · Agent: read-only

- Reads: `docs/02_specification.md`, `docs/02_contracts/`, `docs/03_test-strategy.md`, `docs/03_acceptance-manifest.sha256`, `project/01_requirements/scope.md` (priorities), `general/quality/test-strategy.md`, `general/phases.md` ("TDD cycle", "Constraints")
- Writes: `docs/04_cycle-plan.md`, unit and integration tests, source code, red-run logs in `out/logs/tdd/`

## Procedure

1. **Plan once.** Write `docs/04_cycle-plan.md`: one row per cycle in priority order (storage before notifications, participant flow before organizer features), with its AC or AC range, component, the test classes you will add and the frozen tests the cycle should turn green. Group ACs only when they share one code path. Commit it alone (`docs:`).
2. **Red.** Write the unit or integration tests for the cycle's behaviour, with the AC id or range in each test name. Add only the stubs the tests need to compile (signatures that report "not implemented"). Run only the touched test classes, writing the output to `out/logs/tdd/<cycle>-red.log` in the same command; the new tests must fail on an assertion or a stub. Commit `test: <summary> (AC-…)`.
3. **Green.** Write the least production code that makes the new tests and the cycle's frozen tests pass; change no test. Run the touched test classes and the cycle's frozen tests. Commit `feat: <summary> (AC-…)`. If the code needs more than 15 files or 400 lines, split the cycle by layer into several red → green pairs with the same IDs.
4. **Refactor** only when the code needs it: change code, not tests; run the same tests; commit `refactor: <summary> (AC-…)`.
5. **End of a user story:** run the full suite and the check commands (ES-05), fix anything that fails in a new red → green pair, and update `docs/00_progress.md` (last cycle done, next cycle).
6. **Gate:** when every cycle is done, run the full suite once more and check the phase 4 gate in `general/phases.md`.

A frozen test that cannot pass with a correct implementation is a blocking decision (`general/working-rules.md`); do not touch the test.
