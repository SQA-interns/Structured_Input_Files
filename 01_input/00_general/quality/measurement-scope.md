# Measurement scope

> Owner: QA lead · Read in: phases 3, 5, 6 · Agent: read-only

Defines what test counts, coverage, mutation score and code size mean, so that figures from different runs compare. Record every figure in `run-log.json` → `codeMetrics` and in `docs/06_verification-report.md` with its scope, level and tool. The project selects paths and exclusions in `project/02_design/quality-requirements.md` ("Mutation scope"); this file never names them.

## Constraints

- Do not change a tool's scope, filters, exclusions or thresholds to improve a number. A change is a decision record.
- Do not report a figure without its scope, level and tool.
- Production code is the source a component ships. It excludes tests, generated code, the application entry point and anything listed under "Measurement exclusions" in `docs/03_test-strategy.md` with a reason. Decide the exclusions in phase 3, before any test is written; do not extend them later.
- Test code never counts towards coverage or mutation.

## Test counts

Per component and level (`unit`, `integration`, `acceptance`, `architecture`, `e2e`), passed and failed, from the runner's own report.

## Coverage

Line and branch, per component, as three figures: `unit` (unit tests only), `nonUnit` (integration, acceptance and end-to-end tests that exercise the component) and `all` (one merged report). The headline figure is `all`.

## Mutation score

- **Scope:** all production code of the component, narrowed only to the paths in `project/02_design/quality-requirements.md` "Mutation scope", which are the same in every run of the project (default: all production code). Do not narrow it by running only some tests or by excluding packages that are not listed there.
- **Tests:** the component's full automated suite, at every level it has, so the score does not depend on which level the agent ran. If that is infeasible, record the tests actually run and the reason in a decision record.
- **Record:** `{score, scoreCovered, killed, total, covered, scope, tests, tool, excluded}`. `score` = killed (timed-out counts as killed) ÷ all mutants the tool generated; a mutant no test reaches counts as survived. `scoreCovered` = killed ÷ mutants a test reaches.
- Do not remove equivalent mutants by hand; classify the surviving ones in the verification report.

## Code size

`cloc` (same version in every run): production = non-test source and resources, test = test sources including end-to-end tests. Generated code and lock files are excluded.
