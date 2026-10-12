# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single-page registration form built with Vite; served by nginx (non-root, port 8080), which forwards `/api/` to the backend (AR-01). UI contract: `../docs/02_contracts/ui-form.md`.

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`engine-strict` is on)
- For end-to-end tests: the local stack running (`../scripts/compose.sh up -d --build`) and Playwright's Chromium (cached in `~/.cache/ms-playwright`; otherwise `npx playwright install chromium`, which needs approval on a shared host)

## Commands (ES-05)

| Purpose         | Command                                                                                                                                              |
| --------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| install         | `npm ci`                                                                                                                                             |
| build           | `npm run build` (type check, then `dist/`)                                                                                                           |
| test            | `npm test` (unit and component acceptance tests); coverage: `npx vitest run --coverage`                                                              |
| check           | `npm run check` (Prettier, ESLint, `tsc --noEmit`); format with `npm run format`                                                                     |
| run             | development: `npm run dev` (proxies `/api` to `127.0.0.1:8080`, so run the backend there); container: `../scripts/compose.sh up -d --build frontend` |
| end-to-end      | `E2E_ORGANIZER_USERNAME=… E2E_ORGANIZER_PASSWORD=… npx playwright test` (values from `.env`; optional `E2E_BASE_URL`, `E2E_MAILPIT_URL`)             |
| mutation        | `npx stryker run` (command runner, D-24; about 15 minutes)                                                                                           |
| dependency scan | `npm audit` (shipped code: `npm audit --omit=dev`)                                                                                                   |

## Configuration

The frontend has no build-time configuration and no secret (AR-07). It reads the conference name, consent wording and reCAPTCHA mode and site key at runtime from `GET /api/settings`. Security headers (CSP allowing only Google's reCAPTCHA domains) are set in `nginx.conf`.

## Troubleshooting

| Symptom                                      | Cause and fix                                                                        |
| -------------------------------------------- | ------------------------------------------------------------------------------------ |
| "The registration form could not be loaded." | the backend is not reachable at `/api`; check `../scripts/compose.sh ps`             |
| `npm ci` fails with an engine error          | wrong Node or npm version                                                            |
| e2e tests fail at once                       | stack not running, or `E2E_ORGANIZER_*` not set                                      |
| the reCAPTCHA widget does not appear         | live mode needs `RECAPTCHA_SITE_KEY` on the backend and network access to google.com |
