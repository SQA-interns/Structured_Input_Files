# Progress

> Written in: every phase · Agent: writes

Updated at every gate and before any stop, so a fresh session can resume from here.

- Workflow: tdd
- Current phase: 7 (release) — done
- Current cycle (phase 4): none (C01–C12 done)
- Last gate result: phase 7 passed (2026-10-19): READMEs work from a clean clone (`out/logs/phase7-clean-clone.log`), manual-test list in `docs/release-notes.md`, every decision resolved or pending review (D-08, D-09)
- Next step: none; the run is complete (human review of D-08 and D-09, manual tests in the release notes)
- Waiting for the human on: nothing

## Component commands (ES-05)

| Component | build | test | check | run |
|---|---|---|---|---|
| backend (`02_output/backend`) | `./mvnw -B package -DskipTests` | `./mvnw -B test` | `./mvnw -B spotless:check pmd:check pmd:cpd-check spotbugs:check` | `./mvnw spring-boot:run` |
| frontend (`02_output/frontend`) | `npm run build` | `npm test` | `npm run check` | `npm run dev` |
