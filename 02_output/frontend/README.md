# Frontend

> Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React single page of the conference registration (`docs/02_contracts/ui-contract.md`). It talks to the backend only through relative `/api` URLs and holds no secret; the reCAPTCHA site key comes from `GET /api/form-config`.

## Prerequisites

- Node.js 24.13.0 with npm 11.6.2.
- For end-to-end tests: Playwright's Chromium (`npx playwright install chromium`, once) and the running local stack.

## Configuration

The built page has no settings of its own. Behaviour that differs per environment comes from the backend (`captcha.mode`, `captcha.siteKey`, conference name, options). The container's nginx ([nginx.conf](nginx.conf)) serves the page on port 8080, sets the security headers and proxies `/api/` to `http://backend:8080` with its own `Host` header. The dev server (`npm run dev`) proxies `/api` to `http://127.0.0.1:8080`.

## Build

```sh
npm ci
npm run build                      # type check + production bundle in dist/
docker build -t registration-frontend .
```

## Run

```sh
npm run dev                        # http://127.0.0.1:5173, needs a backend on 127.0.0.1:8080
```

In the local stack the frontend runs as a container on <http://127.0.0.1:8080> (root README).

## Test

```sh
npm test                           # Vitest: unit and acceptance tests (56)
npm run check                      # Prettier, ESLint, tsc --noEmit
npm run coverage                   # coverage report in coverage/
npm run mutation                   # Stryker mutation testing (about 5 minutes)
npm run duplication                # jscpd
```

End-to-end tests (Playwright, against the running stack from the root README). They read the organizer credentials from the environment, so export them from `.env` without printing them:

```sh
export ORGANIZER_USERNAME="$(sed -n 's/^ORGANIZER_USERNAME=//p' ../../.env | tr -d '\r')"
export ORGANIZER_PASSWORD="$(sed -n 's/^ORGANIZER_PASSWORD=//p' ../../.env | tr -d '\r')"
npm run e2e
```

## Troubleshooting

| Symptom                                                                     | Cause and fix                                                                      |
| --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| "The registration form is currently unavailable"                            | `/api/form-config` failed; check that the backend is healthy (`docker compose ps`) |
| e2e: `ORGANIZER_USERNAME and ORGANIZER_PASSWORD must be exported from .env` | export them as shown above                                                         |
| e2e: Mailpit or `docker compose exec` errors                                | start the stack first; Mailpit must listen on 127.0.0.1:8025                       |
| e2e: HTTP 429                                                               | the stack allows 10 registrations per minute per client; wait a minute and re-run  |
| `npx playwright` cannot find Chromium                                       | run `npx playwright install chromium`                                              |
