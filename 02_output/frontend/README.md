# Frontend

> Written in: phases 0 and 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React 19 + Vite registration form (UI contract `docs/02_contracts/ui.md`). It talks to the backend only through relative `/api` URLs (AR-01) and contains no secret; the reCAPTCHA site key comes from `/api/form-config` at run time (AR-07).

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2.
- For end-to-end tests: the local stack running (`docker compose` in `02_output`) and Playwright's Chromium (`npx playwright install chromium` once, if it is not installed).

## Configuration

The bundle has no settings. The container's `nginx.conf` serves the files on port 8080 and forwards `/api/` to `http://backend:8080` with a fixed `Host` (SR-08). The dev server (`npm run dev`) proxies `/api` to `http://127.0.0.1:8080`.

End-to-end tests read `E2E_BASE_URL` (default `http://127.0.0.1:8081`), `E2E_API_URL` (default `http://127.0.0.1:8080`), `E2E_MAILPIT_URL` (default `http://127.0.0.1:8025`) and `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD`, `ORGANIZER_EMAILS` from the environment (values from `.env`).

## Commands (ES-05)

Run from `02_output/frontend` after `npm ci`.

| Purpose                            | Command                                                                                       |
| ---------------------------------- | --------------------------------------------------------------------------------------------- |
| build                              | `npm run build` (type check + `dist/`)                                                        |
| test (unit and acceptance)         | `npm test`                                                                                    |
| check (format, lint, type check)   | `npm run check` (fix formatting with `npm run format`)                                        |
| run (dev server on 127.0.0.1:5173) | `npm run dev` (backend on port 8080 needed)                                                   |
| run (container)                    | from `02_output`: `docker compose --env-file ../.env up -d --build` → <http://127.0.0.1:8081> |
| end-to-end                         | `set -a; . ../../.env; set +a; npm run e2e` (stack running)                                   |
| coverage                           | `npm run coverage`                                                                            |
| mutation                           | `npm run mutation` (report in `reports/mutation`)                                             |
| duplication                        | `npm run duplication`                                                                         |

## Troubleshooting

| Problem                                                          | Fix                                                               |
| ---------------------------------------------------------------- | ----------------------------------------------------------------- |
| The page shows "The registration form is currently unavailable." | the backend is not reachable on `/api`; check `docker compose ps` |
| End-to-end tests time out on the first page                      | start the stack first; check `E2E_BASE_URL`                       |
| Playwright reports a missing browser                             | `npx playwright install chromium`                                 |
