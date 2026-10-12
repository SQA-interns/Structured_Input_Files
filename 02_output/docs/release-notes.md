# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0 of the conference registration system (run `kyuhi_single-agent_sdd_v003`, 2026-10-12).

## Delivered

- **US-001, US-002** External participant and student registration forms with client and server validation, Unicode text (č, š, ž), duplicate-email rejection (D-14) and Google reCAPTCHA v2 (test mode locally).
- **US-003** Options (workshops, events, meals, other activities) from a JSON file validated at startup; options can be limited to one registration type (D-10).
- **US-004** In-application confirmation only after the backend accepted the registration.
- **US-005** Storage in PostgreSQL (Flyway migration) plus an atomic raw JSON copy per registration; success is answered only after both are written.
- **US-006, US-007** Plain-text participant confirmation and organizer notification with the JSON copy attached; an email failure keeps the registration (D-12).
- **US-008** Excel export for the organizer over HTTP Basic (HTTPS-only outside localhost, BCrypt-hashed password, rate limited).
- Security controls: rate and request-size limits, security headers, non-root containers, problem responses without internals, logs without personal data.
- Local stack: `docker-compose.yml` with health checks, started by `scripts/compose.sh`.
- Verification: 177 automated tests (139 backend, 34 frontend, 4 end-to-end) pass; coverage (all levels) backend 95.2 % lines / 84.3 % branches, frontend 97.8 % / 90.0 %; mutation backend 72.6 % (unit and architecture tests, D-23), frontend 88.2 % (D-24). Details: `docs/06_verification-report.md`.

## Known limitations

- Option changes need a backend restart (D-20); there is no administration UI and no deletion feature: registrations and JSON copies must be deleted by the organizer by hand 12 months after the conference (D-15).
- Rate limits are kept in memory per backend instance; several instances would each apply their own limit.
- UI and emails are in English only (D-17).
- The backend image is built from a jar compiled on the host (`./mvnw package`); no JDK build stage, because `tech-stack.md` pins only the JRE image.
- Container images were not vulnerability-scanned: `tech-stack.md` lists no image scanner.
- `npm` 11.6.2 cannot set `min-release-age` (F-05); dependencies are protected by exact pins, the committed lock file and `npm ci`.
- Dev tooling only: two moderate `npm audit` findings under `@stryker-mutator/core` (F-08); the shipped bundle has none.
- Backend mutation testing does not run the container-based tests (D-23); the Stryker Vitest runner does not support Vitest 5, the command runner is used (D-24).
- Six commits exceed the commit size guide (F-10).

## Must be tested manually by a human

1. **reCAPTCHA v2 in production:** with real `RECAPTCHA_SITE_KEY`/`RECAPTCHA_SECRET_KEY` and `RECAPTCHA_TEST_MODE=false`, submit one registration through the widget (accepted) and one with an expired or missing token (rejected).
2. **SMTP delivery:** with the external SMTP server (`MAIL_HOST`, `MAIL_PORT`, `MAIL_TLS=true`, `MAIL_AUTH=true`, `SMTP_*`), check that the participant confirmation and the organizer notification with the JSON attachment reach real mailboxes and show č, š, ž correctly.
3. **TLS and reverse proxy:** HTTPS with the Let's Encrypt certificate, HTTP → HTTPS redirect, `/` served by the frontend, `/api` forwarded to the backend with `X-Forwarded-For` and `X-Forwarded-Proto`; the export works over HTTPS and is refused (403) over plain HTTP.
4. **Production start-up guards:** with `SPRING_PROFILES_ACTIVE=prod`, the backend refuses to start with test mode on, with empty reCAPTCHA keys, or with `OPTIONS_LOCATION` not starting with `file:`.
5. **Production option file and consent wording:** the organizer approves the real option list and the consent text (D-11) in the file named by `OPTIONS_LOCATION`.
6. **Persistence on the production host:** recreate the containers and check that registrations and JSON copies remain (named volumes or host paths writable by uid 1000).
7. **Retention:** delete registrations and JSON copies 12 months after the conference (D-15).

## Decisions pending review

Resolved by the human: D-01 → D-07, D-04 → D-08, D-05 → D-09, D-25 → D-26 (the original records keep "pending"; records are never rewritten).

Chosen by the agent and pending review:

| Decision | Subject |
|---|---|
| D-06 | Medium dependency findings recorded, not blocking |
| D-10 | Options can be limited per registration type (OQ-01) |
| D-11 | One mandatory data-processing consent; wording from configuration (OQ-02) |
| D-12 | Email failure keeps the registration, no retry (OQ-03) |
| D-13 | Any number of options per category, no duplicates (OQ-04) |
| D-14 | Second registration with the same email rejected (OQ-05) |
| D-15 | Retention 12 months after the conference, manual deletion (OQ-06) |
| D-17 | English UI and emails |
| D-18 | Field length limits (200 text, 254 email) |
| D-19 | Rate-limit and request-size defaults |
| D-20 | Option changes need a restart |
| D-21 | SpotBugs EI_EXPOSE_REP2 excluded for injected collaborators |
| D-22 | `scripts/compose.sh` because compose cannot parse `.env` line 2 |
| D-23 | Backend mutation with unit and architecture tests only |
| D-24 | Stryker command runner |
| D-27 | Redocly CLI (contract validation only) not scanned |

Also for the human: change `ORGANIZER_USERNAME` in `.env`; part of it appeared in the session output (F-03).
