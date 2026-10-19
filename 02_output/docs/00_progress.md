# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 7 (release)
- Current cycle (phase 4): none (C01–C12 done)
- Last gate result: phase 6 passed (2026-10-19): every DoD item has evidence (DoD-08, DoD-09 in phase 7), no open Critical/High, hashes match, no secret in files or commit messages
- Next step: phase 7, READMEs (root and per component), release notes, clean-clone check, run summary
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
