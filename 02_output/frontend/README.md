# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single page with the registration form (contract: `../docs/02_contracts/ui.md`). It talks to the backend only through relative `/api` URLs; nginx (container) or the Vite dev server (development) forwards them to the backend.

## Prerequisites

- Node 24.13.0 with npm 11.6.2 (`engine-strict` is on).
- For the end-to-end tests: the local stack running (`../README.md`) and Playwright's Chromium (`npx playwright install chromium` once, if it is not in the user cache).

## Configuration

The frontend has no build-time settings and no secret. The conference name, options, consents and the reCAPTCHA site key and test mode come from `GET /api/form-config` at run time (AR-07). nginx settings (security headers, `/api` proxy, body limit) are in `nginx.conf`.

The end-to-end tests read `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` and `ORGANIZER_EMAILS` from the environment; export them from `.env` without printing them, for example in Git Bash:

```bash
export ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env | tr -d '\r')" ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env | tr -d '\r')" ORGANIZER_EMAILS="$(sed -n 's/^ORGANIZER_EMAILS=//p' ../../.env | tr -d '\r')"
```

## Build

```bash
npm ci && npm run build
```

Output in `dist/`; the image (`Dockerfile`, nginx as a non-root user on port 8080) is built by `docker compose build frontend`.

## Run

```bash
npm run dev
```

Vite on <http://127.0.0.1:5173>, forwarding `/api` to a backend on `127.0.0.1:8080`. With the local stack the frontend is served on <http://127.0.0.1:8080>.

## Test

```bash
npm test
```

Unit and component tests plus the frozen acceptance tests (`src/acceptance`) with Vitest and jsdom. Coverage: `npm run coverage` (`coverage/`). Mutation testing: `npm run mutation` (`reports/mutation/`). End-to-end tests against the running stack: `npm run e2e` (Playwright, `e2e/`).

## Check

```bash
npm run check
```

Prettier, ESLint and TypeScript (`tsc --noEmit` for `src` and `e2e`). Duplication: `npm run duplication`. Dependency scan: `npm audit` (and `npm audit --omit=dev` for the shipped dependencies).

## Troubleshooting

- `npm ci` fails with an engine error: use Node 24.13.0 / npm 11.6.2.
- The page shows "The registration form could not be loaded": the backend is not reachable through `/api` (stack not running, or the dev server proxy points at the wrong port).
- E2E tests fail at once with "export ORGANIZER_USERNAME…": export the organizer variables as shown above.
- E2E tests time out on the form: the stack is not up; check `docker compose --env-file ../../.env ps` in `02_output`.
