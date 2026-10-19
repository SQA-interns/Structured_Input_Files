# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 4 (TDD build)
- Current cycle (phase 4): C08 next (C01–C07 done: every backend acceptance test green)
- Last gate result: phase 3 passed (89 frozen tests, manifest committed in 0b8e69e); after C07 the full backend suite passes (201 tests) and backend checks are clean
- Next step: C08 reCAPTCHA production verifier and SR-02, C09 limits/headers/health, then frontend cycles C10–C12 and the e2e run
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
