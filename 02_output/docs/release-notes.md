# Release notes

> Written in: phase 7 · Agent: writes

Release 1.0.0 of the conference registration (backend `registration-backend` 1.0.0, frontend `registration-frontend` 1.0.0).

## Delivered

- US-001 / US-002: external participant and student registration forms with their fixed fields, trimmed and validated on the client and on the server; Unicode names (č, š, ž) end to end.
- US-003: options (workshops, events, meals, other activities) and consents from a JSON configuration file, validated at startup; options can be limited to one registration type (D-05).
- US-004: confirmation page only after the backend accepted the registration; field errors next to the fields; general errors without technical details.
- US-005: storage in PostgreSQL (Flyway schema) and a raw JSON copy per registration on a persistent volume, in one transaction (no partial registration).
- US-006 / US-007: plain-text confirmation email to the participant and notification to every organizer with the JSON copy attached, sent after storage.
- US-008: Excel export for the organizer (HTTP Basic, BCrypt-hashed password, refused over plain HTTP outside localhost).
- Security: server-side reCAPTCHA v2, production start refused with unsafe settings, per-client rate limits and 16 KiB request limit, security headers, non-root containers, health and readiness probes.
- Delivery: two container images, local `docker-compose.yml`, production reverse proxy example.

Quality at release: 218 automated tests green (backend 189, frontend 27, end to end 2); backend coverage 99.0 % line / 93.1 % branch, mutation 100 % (320 mutants); frontend 90.0 % / 96.0 %, mutation 85.9 %; no open Critical, High or Medium finding (`docs/06_verification-report.md`).

## Known limitations

- No editing, cancelling or deleting registrations in the application; the operator deletes rows and JSON copies directly (retention D-10).
- No administration UI: options and consents change by editing the configuration file and restarting the backend.
- Rate limits are held in memory per backend instance; several instances would each count separately.
- Emails are sent once; a failed send is logged with the registration id and must be resent by the operator from the JSON copy (D-07).
- A second registration with the same email is accepted (D-09); organizers see both in the export.
- The frontend mutation run does not include the end-to-end tests (D-15).

## Must be tested manually by a human

| What | Why | How |
|---|---|---|
| Google reCAPTCHA v2 with the production site and secret keys | automated tests use test mode and a mocked verification endpoint (`environments.md`) | one real registration in production; one submission without solving the widget must be refused |
| Email delivery through the production SMTP server | tests use Mailpit | register with a real mailbox; check the confirmation, the organizer notification and its JSON attachment, and the č/š/ž characters |
| TLS, HTTP→HTTPS redirect and `/api` routing at the external nginx | no reverse proxy in automated tests | open `http://` (redirect), `https://` (form), `https://…/api/form-config`; export over HTTPS with the organizer login; `curl -H 'Host: evil.example'` must not change the redirect target |
| Organizer export over the production proxy | HTTPS-only guard depends on the proxy's `X-Forwarded-Proto` | export over HTTPS works; the same request to the backend over plain HTTP from another host is refused (403) |
| The production conference configuration file and consent wording | content is the product owner's (D-06) | load the real file; check options, availability per type and the consent text on the page |
| Opening the exported workbook in Excel / LibreOffice | automated checks parse the file with Apache POI | open `registrations.xlsx`; check columns and Slovenian characters |

## Decisions pending review

| Decision | Topic | Chosen behaviour |
|---|---|---|
| D-05 | OQ-01 options per registration type | per-option `availableTo` list, default both types |
| D-06 | OQ-02 consents and wording | one mandatory `privacy` consent, wording to be confirmed |
| D-07 | OQ-03 email failure after storage | registration stays accepted; failure logged by id |
| D-08 | OQ-04 options per category | no limit; duplicates rejected |
| D-09 | OQ-05 same email twice | allowed |
| D-10 | OQ-06 retention | kept until the operator deletes them, at most 12 months after the conference |
