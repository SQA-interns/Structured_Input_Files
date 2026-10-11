# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React (Vite) single-page registration form: type choice (external participant or student), fields, options grouped by category, consents, reCAPTCHA v2, confirmation. Talks to the backend only through relative `/api` URLs (AR-01).

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2 (`node --version`, `npm --version`).
- For end-to-end tests: Playwright's Chromium (`npx playwright install chromium`, downloads the browser bundled with @playwright/test 1.63.0) and the running local stack.

Install dependencies (exact versions from `package-lock.json`):

```bash
npm ci
```

## Configuration

The frontend has no secret (AR-07). Everything it needs comes from the backend at runtime through `GET /api/form-config`: conference name, active options, consents, the public reCAPTCHA site key and whether test mode is on. The container's nginx forwards `/api/` to the service named `backend` on port 8080 (`nginx.conf`). Security headers (CSP, framing, content type, referrer) are set in `nginx.conf`.

| Setting                             | Where                                                            | Default                 |
| ----------------------------------- | ---------------------------------------------------------------- | ----------------------- |
| Backend for `npm run dev`           | `vite.config.ts` proxy                                           | `http://127.0.0.1:8080` |
| Base URL for e2e tests              | `E2E_BASE_URL`                                                   | `http://127.0.0.1:8081` |
| Mailpit for e2e tests               | `E2E_MAILPIT_URL`                                                | `http://127.0.0.1:8025` |
| Organizer credentials for e2e tests | `E2E_ORGANIZER_USERNAME`, `E2E_ORGANIZER_PASSWORD` (from `.env`) | none                    |

## Build

```bash
npm run build
```

Type-checks and writes the static site to `dist/`. Container image: `docker build -t registration-frontend .`

## Run

Development server on http://127.0.0.1:5173 (needs the backend on 127.0.0.1:8080):

```bash
npm run dev
```

As a container with the local stack, from `02_output/`: `docker compose --env-file ../.env up -d --build --wait` (form on http://127.0.0.1:8081).

## Test

```bash
npm test
```

Unit, component and acceptance tests (Vitest, jsdom). Coverage: `npm run test:coverage`. Mutation testing: `npm run mutation` (Stryker; report in `reports/mutation/`).

End-to-end tests against the running local stack (bash):

```bash
E2E_ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env | tr -d '\r')" E2E_ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env | tr -d '\r')" npm run test:e2e
```

## Check

```bash
npm run check
```

Prettier, ESLint and `tsc --noEmit`. Duplication: `npm run duplication`. Dependency scan: `npm audit`.

## Troubleshooting

| Symptom                                      | Cause and fix                                                                                                      |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| "The registration form could not be loaded." | `/api/form-config` is not reachable: start the backend (dev) or check the `backend` container (compose).           |
| No reCAPTCHA widget in production            | The CSP blocks Google or the site key is empty; check `RECAPTCHA_SITE_KEY` on the backend and the browser console. |
| e2e tests fail on the export                 | `E2E_ORGANIZER_USERNAME`/`PASSWORD` not set or different from the backend's `.env`.                                |
| e2e tests cannot connect                     | The local stack is not running on 127.0.0.1:8081 / 8025.                                                           |
