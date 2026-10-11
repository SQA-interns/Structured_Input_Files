# Frontend

> Written in: phases 0 and 7 · Source: `project/02_design/architecture.md`, ES-05, ES-06 · Agent: writes

React + Vite registration form; talks to the backend only through `/api` (AR-01).

## Commands (ES-05)

Run from `02_output/frontend` after `npm ci`.

| Purpose | Command |
|---|---|
| build | `npm run build` |
| test | `npm test` |
| check (format, lint, type check) | `npm run check` |
| run | `npm run dev` (proxies `/api` to `http://127.0.0.1:8080`) |
