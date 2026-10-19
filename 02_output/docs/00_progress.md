# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 5 (quality gate)
- Current cycle (phase 4): none (C01–C12 done)
- Last gate result: phase 4 passed (2026-10-19): frozen tests green (77 backend acceptance, 7 frontend acceptance, 5 e2e), full suites 232 backend + 56 frontend pass, checks clean, `trace-ac.mjs`: no AC without a test, all test-first (`out/logs/phase4-trace-ac.log`)
- Next step: phase 5, TDD baseline coverage (JaCoCo, Vitest v8) before any added test, then first full run, then mutation (PIT, Stryker)
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
