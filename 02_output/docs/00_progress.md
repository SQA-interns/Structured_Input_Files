# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 3 (acceptance tests)
- Current cycle (phase 4): none
- Last gate result: phase 2 passed (2026-10-19): every AC assigned to a component and contract, every SB, SR, NFR mapped, AR-02 and AR-04 declared, contracts validated (`out/logs/phase2-contract-validation.log`)
- Next step: phase 3, backend acceptance harness and tests story by story, then frontend, then e2e, then the freeze commit
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
