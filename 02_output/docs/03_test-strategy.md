# Test strategy (as executed)

> Written in: the test phases of the selected workflow, and phase 6 · Source: `general/quality/test-strategy.md`, workflow `rules.md` · Agent: writes

## Test levels as executed

## Measurement exclusions

<!-- Phase 3, before any test is written: production code left out of coverage and mutation, one row per path with the reason (generated code, application entry point, …). Rules and defaults: `general/quality/measurement-scope.md`. Not extended later without a decision record. -->

| Path | Reason |
|---|---|
| `backend/src/main/java/si/konferenca/registration/RegistrationApplication.java` | application entry point (`measurement-scope.md` default) |
| `frontend/src/main.tsx` | application entry point (mounts `App`) |
| `frontend/src/vite-env.d.ts` | type declarations only, no executable code |

Mutation scope: `quality-requirements.md` lists no paths, so the default applies: all production code of each component minus the rows above. Backend: PIT over `si.konferenca.registration.*`; frontend: Stryker over `src/**/*.ts(x)` without tests.

## First complete run (before any fix)

## Final run (phase 6)
