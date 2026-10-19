# Phases (test-driven: frozen acceptance tests, then red-green-refactor)

> Owner: Team lead · Read in: every phase · Agent: read-only

Run the phases in order. A phase starts only after the previous gate has passed.
Adds to `general/working-rules.md`; does not replace it. Path aliases: see `AGENTS.md`.

Two loops: the outer loop is the frozen acceptance and end-to-end tests of phase 3; the inner loop is the TDD cycles of phase 4, where every unit or integration test is written and seen failing before the production code that makes it pass.

| # | Phase | Reads | Writes | Gate (must be true to continue) |
|---|---|---|---|---|
| 0 | Preflight & bootstrap | `project/00_setup/*`, `general/engineering-standards.md`, `general/security/security-baseline.md`, `general/quality/severity-scale.md`, skill `general/skills/preflight` | `docs/00_preflight-report.md`, `docs/00_input-manifest.sha256`, `docs/00_progress.md`, one skeleton per component, repo files (ES-03) | Every preflight check passes; every component builds; manifests and lock files match `tech-stack.md` exactly (as amended by approved decision records); every listed tool runs; input manifest written |
| 1 | Requirements | `project/01_requirements/*` (the acceptance criteria and end-to-end scenarios are inputs and are not changed) | `docs/01_requirements-check.md` (story → criteria; gaps only, nothing copied) | Every story has ≥ 1 AC in the given list; every AC cites a story; every open question has an answer in `scope.md`; a gap or contradiction in the inputs is a decision record |
| 2 | Design (contracts only) | `project/01_requirements/*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/*`, `general/security/*` | `docs/02_specification.md`, `docs/02_contracts/` | Every AC is assigned to a component and an interface; every SR, SB and NFR maps to a control or target; the backend architecture is declared for AR-02 and the options mechanism for AR-04; contracts validate with a parser |
| 3 | Acceptance tests | `project/01_requirements/acceptance-criteria.md`, `project/01_requirements/e2e-scenarios.md`, `docs/02_contracts/`, `general/quality/test-strategy.md`, `general/quality/measurement-scope.md`, skill `general/skills/write-acceptance-tests` | acceptance tests, `docs/03_test-strategy.md`, `docs/03_acceptance-manifest.sha256` | Every AC has a test; every e2e scenario has exactly one e2e test and there is no other e2e test; every test fails for a behavioural reason (no build or setup errors), or passes only because bootstrap code already provides the behaviour, listed in `docs/03_test-strategy.md`; manifest committed last, after all tests, as its own freeze commit |
| 4 | TDD build | `docs/02_*`, `docs/03_test-strategy.md`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `project/02_design/architecture.md`, `general/engineering-standards.md`, `general/quality/test-strategy.md`, skill `general/skills/tdd-cycle` | `docs/04_cycle-plan.md`, unit and integration tests, source code | All frozen acceptance tests pass; every AC has ≥ 1 unit or integration test committed in a `test:` commit before the first `feat:` commit of its cycle; no `feat:` commit without an earlier `test:` commit naming the same IDs; every red, green and refactor commit made by `general/skills/tdd-cycle/tdd-step.mjs`; full suite passes; format, lint and type checks are clean; at least one cycle per user story |
| 5 | Quality gate | source code, tests, `general/quality/test-strategy.md`, `general/quality/measurement-scope.md` | added tests, `docs/03_test-strategy.md` | TDD baseline recorded before any added test; first full run recorded and classified before any fix; full suite passes; **quality gate** met for each component as `general/quality/measurement-scope.md` ("Quality gate") defines: line coverage and adjusted mutation score at or above the thresholds in `project/02_design/quality-requirements.md`, or the shortfall recorded after the third survivor round; no further tests for a component once it meets the gate |
| 6 | Verify | all of `01_input/`, all of `docs/`, skill `general/skills/verify-release` | `docs/06_verification-report.md` | Every general and project DoD item has evidence; no open Critical/High finding; manifest hashes match; no secret value found in any file, log or commit message; coverage and mutation score re-measured only for a component whose code or tests changed since phase 5, once, after the last fix |
| 7 | Release | `general/quality/definition-of-done.md`, `general/engineering-standards.md`, `project/02_design/architecture.md` | `out/README.md`, one README per component, `docs/release-notes.md` | Every README works when followed from a clean checkout; manual-test list present; every decision has a resolution or is listed as pending review |

Any phase may write `docs/decisions-log.md`, `docs/00_progress.md` and `out/logs/`.

## On a failed gate

- Fix within the same phase, then re-check the gate.
- If the gate needs a change to an input or a frozen test, stop and raise a decision record.

## Test levels

| Level | Written in phase | Written from | Frozen | Location |
|---|---|---|---|---|
| Acceptance | 3 | acceptance criteria + contracts | yes, from the freeze commit | path contains `acceptance` |
| End-to-end | 3 | `project/01_requirements/e2e-scenarios.md` (one test per scenario) | yes, from the freeze commit | path contains `e2e` |
| Unit / integration (test-first) | 4, red step of a cycle, before the code | the behaviour the cycle's ACs need | no | any other test path |
| Unit / integration (test-after) | 5, only for coverage gaps and surviving mutants | coverage and mutation reports | no | any other test path |

## TDD cycle

- A cycle covers one AC, or a range of ACs of one story that share one code path (`AC-001-02..07`). The cycles, their order (priorities in `project/01_requirements/scope.md`) and the frozen tests each one turns green are planned once in `docs/04_cycle-plan.md` at the start of phase 4.
- Red: the new tests fail for a behavioural reason (an assertion, or a stub that reports "not implemented"), never because of a compile, build or harness error.
- Green: the least production code that makes the new tests and the cycle's frozen tests pass, without changing a test.
- Refactor (when needed): code changes only; every test still passes and no test changes.
- Build and configuration files (`build:`, `chore:`) need no red test; behaviour under a component's source folder does.

## Commit units

| Phase | One commit per |
|---|---|
| 0 | component build file (pinned dependencies) and, separately, its source skeleton; repository files (ES-03); the preflight report, input manifest and progress file together (one commit) |
| 1, 2 | the requirements check (one commit); the specification with all its contracts (one commit) |
| 3 | user story's acceptance and end-to-end tests, then the freeze commit (manifest only) |
| 4 | the cycle plan (one `docs` commit); then per cycle, each step committed by `tdd-step.mjs`: `test:` (the failing tests, plus stubs needed to compile), then `feat:` (the code that makes them pass), then `refactor:` only when needed; a cycle over 15 files or 400 lines is split by layer into several `test:` → `feat:` pairs with the same IDs |
| 5 | group of test classes of one area or layer, at most 400 lines (for example domain rules, adapters, web filters, UI screen); tool configuration (Stryker, JaCoCo) in its own `build` commit |
| 6 | fix of one finding, with its re-run evidence; the report in its own commit |
| 7 | README, release notes |

## Constraints

- Do not read or write production source code in phase 3.
- Do not write production code in phase 4 without a failing test, committed just before it, that requires it.
- Do not change a test in a `feat:` or `refactor:` commit.
- Do not start the next cycle while any test fails, other than frozen tests of behaviour not yet built.
- In the red and green steps run only the test classes the cycle touches plus its frozen tests; run the full suite at the end of each user story and at the phase 4 gate.
- Do not write test-after tests in phase 4, and in phase 5 only for coverage gaps and surviving mutants.
- Do not edit, delete or weaken a test listed in `docs/03_acceptance-manifest.sha256`.

## Evidence (checked in phase 6)

- The phase 3 commits add no production code beyond the bootstrap skeleton; the freeze commit adds the manifest, and every file it lists was committed earlier in phase 3.
- For every AC, a `test:` commit naming it (or its range) precedes the first `feat:` commit naming it, and the red run of the cycle is logged in `out/logs/tdd/` by `tdd-step.mjs` (`general/skills/verify-release/trace-ac.mjs` prints the order).
- Build has at least one cycle per user story; no commit exceeds 15 files or 400 lines or mixes code, tests, documents and build files, except the stubs of a `test:` commit (`working-rules.md`, Commits). The report measures message length, bursts and size on every commit, so the agent does not audit commit subjects itself.
- Every hash in the manifest still matches.
