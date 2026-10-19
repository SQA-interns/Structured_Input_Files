# Release notes

> Written in: phase 7 · Agent: writes

Version 1.0.0 (2026-10-19).

## Delivered

- Registration form for external participants and students (US-001, US-002): only the fields of the chosen type, the active options available to that type grouped as workshops, events, meals and other activities, unchecked consents, reCAPTCHA v2 (test mode locally).
- Options and consents configured in a JSON file (`CONFERENCE_CONFIG_FILE`, schema `docs/02_contracts/conference-config.schema.json`); a changed file and a restart change the form; an invalid file stops the start (US-003, AR-04).
- Validation on the backend (required fields, trimming, email format, option availability, duplicates, mandatory consent, reCAPTCHA) with field errors shown next to the fields; client-side checks before sending (US-004, NFR-03).
- In-application confirmation after acceptance (US-004).
- Storage in PostgreSQL (Flyway schema) and a raw JSON copy per registration in one transaction; a failed copy rolls the row back (US-005, AR-05).
- Participant confirmation email and organizer notification with the JSON copy attached, sent after the registration is stored; a mail failure keeps the registration (US-006, US-007, OQ-03).
- Excel export for organizers with HTTP Basic, HTTPS-only except from localhost (US-008, SR-06).
- Rate limits per client, 16 KiB body limit, security headers, non-root containers, health and readiness probes.
- Local stack with `docker-compose.yml`; 349 automated tests (backend 264, frontend 80, end-to-end 5), line coverage 97.64 % / 100 %, mutation score (adjusted) 100 % / 100 %.

## Known limitations

- The UI text is English (D-08, pending review).
- Status codes and limits the requirements left open were chosen conservatively: 403 for organizer credentials over plain HTTP from a remote client; 60 / 10 / 10 requests per minute and client for form configuration / registration / export; 16 KiB bodies; maximum lengths 100 (names), 254 (email), 200 (other text) (D-09, pending review). All are configurable.
- Rate limits are kept in memory per backend instance; they reset on restart and are not shared between instances.
- The container base images are not scanned for vulnerabilities in this build (no image scanner in `tech-stack.md`, F-07).
- `npm audit` reports two Moderate advisories in dev-only tooling (`qs` via Stryker, PA-03); the shipped bundle has none.
- Registrations and JSON copies are never deleted automatically (OQ-06). **The operator must delete the database rows and the JSON copies no later than 12 months after the conference.**

## Must be tested manually by a human

1. reCAPTCHA in production: one real submission with the production site and secret keys (`RECAPTCHA_TEST_MODE=false`, `APP_ENVIRONMENT=production`); a submission without solving the widget must be rejected.
2. SMTP: delivery of the participant confirmation and the organizer notification (with the JSON attachment) to real mailboxes through the external SMTP server with STARTTLS (`MAIL_TLS=true`, `SMTP_USERNAME`, `SMTP_PASSWORD`).
3. TLS and the external nginx reverse proxy: HTTPS with Let's Encrypt, redirect from HTTP, `/` to the frontend and `/api` to the backend, `X-Forwarded-Proto` and `X-Forwarded-For` set by the proxy, and the upstream `Host` set by the proxy, never forwarded from the client (SR-08); the export works over HTTPS and is refused over plain HTTP.
4. The organizer export opened in Excel or LibreOffice: Slovenian characters (č, š, ž) displayed correctly.
5. A vulnerability scan of the container images used in production (postgres 16.15-alpine, eclipse-temurin 21.0.10_7-jre-alpine, nginx 1.30.5-alpine) with the operator's image scanner.
6. Accessibility of the form with a screen reader (labels, field errors announced).

## Decisions pending review

| Decision | Subject |
|---|---|
| D-08 | UI text in English |
| D-09 | 403 for organizer access over plain HTTP; rate, size and length limits; unknown JSON properties rejected; fields of the other type ignored |

Every other decision in `docs/decisions-log.md` is resolved.
