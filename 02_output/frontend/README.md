# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

React 19 single-page registration form built with Vite 6 and served by nginx 1.30 as non-root on port 8080. It talks to the backend only through relative `/api` URLs (AR-01); in the container nginx forwards `/api` to `backend:8080`. UI contract: [../docs/02_contracts/ui-registration-form.json](../docs/02_contracts/ui-registration-form.json).

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2.
- For the end-to-end tests: the local stack running (`docker compose` in `02_output/`) and Playwright's Chromium (`npx playwright install chromium`).

## Configuration

The frontend has no build-time settings and no secrets (AR-07). At runtime it reads `GET /api/form-config`: conference name, options, consents and the reCAPTCHA site key and test mode, all from the backend configuration. With test mode on it shows "I am not a robot (test mode)" instead of the Google widget.

For the Vite dev server (`npm run dev`, port 5173) `/api` is proxied to `http://127.0.0.1:8080`; a backend started outside Docker needs `APP_CORS_ALLOWED_ORIGIN` only when called from another origin.

## Build, test, check, run (ES-05)

| Task                             | Command (in `02_output/frontend`)                        |
| -------------------------------- | -------------------------------------------------------- |
| install                          | `npm ci`                                                 |
| build                            | `npm run build` (type check and Vite build into `dist/`) |
| test (unit and acceptance)       | `npm test` (coverage: `npm run test:coverage`)           |
| check (format, lint, types)      | `npm run check`                                          |
| run (dev server)                 | `npm run dev`                                            |
| end-to-end tests (stack running) | `npm run test:e2e`                                       |
| mutation testing                 | `npm run mutation`                                       |
| duplication                      | `npm run duplication`                                    |

Container image: `docker build -t registration-frontend .` (multi-stage: Node build, nginx runtime with the security headers of `nginx.conf`).

The end-to-end tests read the organizer credentials and recipients from the environment or from the repository-root `.env` and inspect PostgreSQL and the JSON copies with `docker compose exec`; Mailpit is expected at `http://127.0.0.1:8025` (`MAILPIT_URL`), the page at `http://127.0.0.1:8080` (`E2E_BASE_URL`).

## Troubleshooting

| Symptom                                     | Cause and fix                                                                               |
| ------------------------------------------- | ------------------------------------------------------------------------------------------- |
| "The registration form could not be loaded" | the backend is not reachable under `/api`; check `docker compose ps` and the backend logs   |
| e2e tests fail with `psql failed`           | the stack is not running or was started from another folder; start it from `02_output/`     |
| Playwright cannot find a browser            | run `npx playwright install chromium`                                                       |
| reCAPTCHA widget missing in production      | the site key is empty or the CSP blocks Google; check `RECAPTCHA_SITE_KEY` and `nginx.conf` |
