# Release notes

> Written in: phase 7 · Agent: writes

Release 1.0.0 of the conference registration (backend `registration-backend` 1.0.0, frontend `registration-frontend` 1.0.0), 2026-10-12.

## Delivered

- US-001, US-002: registration forms for external participants and students with the fixed fields of BR-01, options grouped as workshops, events, meals and other activities, only active options available to the type, unchecked consents, client and server validation, reCAPTCHA v2 (test mode locally).
- US-003: options and consents configured in a JSON file (`CONFERENCE_CONFIG`), validated at startup; the application refuses to start with an invalid file.
- US-004: in-page confirmation with the participant's name and selected options only after the backend accepted; field errors next to the fields; a general message on backend failure.
- US-005: storage in PostgreSQL (Flyway migration V1) and a raw JSON copy per registration, written in one unit (no success without both).
- US-006, US-007: plain-text confirmation email to the participant; notification to every organizer address with the JSON copy attached. Email failures never undo an accepted registration.
- US-008: Excel export for the organizer (HTTP Basic, BCrypt-stored password, HTTPS-only except from localhost).
- Security: rate limits and body limit, security headers, non-root containers, health and readiness probes.
- Verification: 298 automated tests (backend 241, frontend 52, end-to-end 5); quality gate met (backend line 99.24 % / mutation 91.44 %, frontend line 100 % / mutation 90.24 %). See `docs/06_verification-report.md`.

## Known limitations

- Rate limits are kept in memory per backend instance; several instances behind a load balancer each count separately.
- Emails are sent synchronously after storing; a slow mail server delays the answer by up to the 5 s timeouts (the registration is still accepted).
- The JSON copy and the organizer attachment use LF line ends and two-space indentation; old copies are never rewritten.
- There is no administration UI, no editing or cancelling of registrations, no automatic deletion (scope).
- Mutation runs do not include the end-to-end tests (D-16).
- Semgrep note F-02: `.npmrc` sets no minimum release age; versions are pinned exactly instead.

## Data retention (OQ-06)

Registrations and JSON copies are kept until the operator deletes them; nothing is deleted automatically. **Delete the database rows (`registration`, `registration_option`, `registration_consent`) and the JSON copy files no later than 12 months after the conference.** Also delete the Mailpit/SMTP mailbox copies that contain personal data.

## Must be tested manually by a human

1. **Google reCAPTCHA v2 with production keys** (`environments.md`): with `RECAPTCHA_TEST_MODE=false` and real keys, one real submission through the widget is accepted, and a submission without solving the widget is rejected.
2. **Delivery to a real mailbox** over the production SMTP server: the participant confirmation and the organizer notification (with the JSON attachment) arrive, render as plain text, and Slovenian characters are intact.
3. **TLS and reverse proxy**: HTTPS with the Let's Encrypt certificate, HTTP redirect to HTTPS, `/` served by the frontend, `/api` routed to the backend, the upstream `Host` set by the proxy (SR-08), and the export refused over plain HTTP from a remote client.
4. **Production startup guard**: the backend refuses to start with `RECAPTCHA_TEST_MODE=true` or with empty reCAPTCHA keys in production.
5. **Excel workbook** opened in Excel or LibreOffice: one row per registration, Slovenian characters, formula-like text shown as text.
6. **Browser check** of the form on a phone-sized screen and with a keyboard only (labels, error messages next to fields).
7. **Backups**: the named volumes `pgdata` and `json-copies` are included in the server backup.

## Decisions pending review

| Decision | Pending choice |
|---|---|
| D-10 | the export over plain HTTP from a non-local client answers 403 (not a redirect) |
| D-11 | a JSON copy that cannot be written answers 500 with a generic message |
| D-12 | fields of the other registration type are rejected with a field error, not ignored |

All other decisions (D-01 … D-09, D-13 … D-16) are resolved; see `docs/decisions-log.md`.
