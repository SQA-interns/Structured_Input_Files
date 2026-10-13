# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single page (Vite) with the registration form, client-side validation, backend field errors next to the fields, and the confirmation. It talks to the backend only through `/api` (AR-01). UI contract: `../docs/02_contracts/ui.md`.

## Prerequisites

- Node.js 24 with npm 11 (exact versions in `tech-stack.md`); install dependencies with `npm ci`.
- For end-to-end tests: the local stack (`docker compose up` in `..`) and Playwright's Chromium (`npx playwright install chromium` once).

## Configuration

The frontend has no secrets and no build-time settings (AR-07). The conference name, options, consents, the reCAPTCHA site key and the test-mode flag come from `GET /api/form-config` at runtime. In development, Vite proxies `/api` to `http://127.0.0.1:8080`; in the container, nginx (`nginx.conf`) proxies `/api/` to `http://backend:8080` and sets the security headers.

End-to-end tests read `ORGANIZER_USERNAME` and `ORGANIZER_PASSWORD` from the environment or from the repository-root `.env`; `E2E_BASE_URL` (default `http://127.0.0.1:8081`) and `E2E_MAILPIT_URL` (default `http://127.0.0.1:8025`) may be overridden.

## Commands (ES-05)

| Purpose         | Command                                                                                              |
| --------------- | ---------------------------------------------------------------------------------------------------- |
| Install         | `npm ci`                                                                                             |
| Build           | `npm run build` (type check, then static files in `dist/`)                                           |
| Test            | `npm test` (unit and acceptance tests, Vitest)                                                       |
| Check           | `npm run check` (Prettier, ESLint, `tsc --noEmit`)                                                   |
| Run             | `npm run dev` (http://127.0.0.1:5173, backend on 8080) or the `frontend` service of `docker compose` |
| End-to-end      | `npm run e2e` (Playwright against the running local stack)                                           |
| Coverage        | `npm run coverage`                                                                                   |
| Mutation        | `npm run mutation` (Stryker, report in `reports/mutation`)                                           |
| Duplication     | `npm run duplication` (jscpd)                                                                        |
| Dependency scan | `npm audit`                                                                                          |

## Troubleshooting

| Symptom                                                      | Cause and fix                                                                          |
| ------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| Page shows "The registration form is currently unavailable." | `/api/form-config` failed: start the backend (dev server) or check `docker compose ps` |
| E2E tests fail with `ORGANIZER_USERNAME is not set`          | export the organizer login or create the repository-root `.env`                        |
| E2E tests fail on `docker compose exec`                      | the stack is not running; start it in `..`                                             |
| Playwright cannot find a browser                             | run `npx playwright install chromium`                                                  |
