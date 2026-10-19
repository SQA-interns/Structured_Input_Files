# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 1 (requirements)
- Current cycle (phase 4): none
- Last gate result: phase 0 passed (2026-10-19): every preflight check passes (`docs/00_preflight-report.md`), both skeletons build, manifests and lock files match `tech-stack.md`, every listed tool runs, input manifest written
- Next step: write `docs/01_requirements-check.md`
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
