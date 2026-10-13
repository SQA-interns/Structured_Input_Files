# Release notes

> Written in: phase 7 · Agent: writes

Version 1.0.0 of the conference registration (backend and frontend).

## Delivered

- Registration forms for external participants and students with the fixed fields of BR-01, active options grouped as workshops, events, meals and other activities, and the mandatory `privacy` consent (US-001, US-002, OQ-01, OQ-02).
- Options and consents from a JSON configuration file; an invalid file stops the backend (US-003, AR-04).
- Confirmation in the page only after acceptance; field errors next to the fields; general error without details (US-004).
- Storage in PostgreSQL (Flyway migration V1) plus a raw JSON copy per registration, written in one unit (US-005, AR-05).
- Participant confirmation email and one organizer notification per address with the JSON copy attached (US-006, US-007); email failures keep the registration (OQ-03).
- Excel export for the organizer over HTTP Basic, HTTPS-only for non-local clients (US-008, SR-06).
- reCAPTCHA v2 verified on the backend; test mode only by configuration; production refuses unsafe settings (SR-01, SR-02).
- Rate limits, 16 KiB body limit, security headers, non-root containers, health and readiness endpoints (SR-03, SB-10, SB-11, NFR-04).
- Quality: 290 automated tests green (backend 216, frontend 69, end-to-end 5); line coverage 99.32 % / 100 %, mutation score 95.72 % / 94.83 % (backend / frontend).

## Known limitations

- Retention (OQ-06): registrations and JSON copies are never deleted automatically. **The operator must delete the database rows (`registration`, cascading to options and consents) and the files in the JSON copy volume no later than 12 months after the conference.**
- Rate limits are kept in memory per backend instance; with several instances each counts separately, and a restart resets them.
- The client address used for rate limits and the HTTPS-only rule comes from `X-Forwarded-For` / `X-Forwarded-Proto` only when the proxy is a trusted internal address (Tomcat defaults: 10/8, 172.16/12, 192.168/16, 127/8). If the production proxy has another address, set `server.tomcat.remoteip.internal-proxies` accordingly.
- With an unreachable database the production start fails on the database before the safety check runs (F-07); with the database reachable, unsafe settings are refused.
- Emails are sent synchronously after storage (D-11); a slow SMTP server delays the answer by up to the 5-second timeouts.
- The backend image needs the jar built on the host first (`./mvnw package`), because only the JRE image is in the tech stack.
- Accepted findings: Semgrep `npm-missing-minimum-release-age` (F-02, needs npm ≥ 11.10), npm audit moderate advisories in dev-only Stryker dependencies (F-05).

## Must be tested manually by a human

| What | How |
|---|---|
| Google reCAPTCHA v2 with production keys | set `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY`, `RECAPTCHA_TEST_MODE=false`; submit one real registration in a browser (the widget appears and the registration is accepted), and one with an expired token (rejected) |
| Email delivery through the real SMTP server | set `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` (STARTTLS on in `prod`); register with a real mailbox; check the confirmation and the organizer notification with its JSON attachment, including Slovenian characters |
| TLS and the reverse proxy | external nginx with Let's Encrypt: HTTP redirects to HTTPS, `/` serves the frontend, `/api` reaches the backend; the proxy sets `Host` itself and overwrites `X-Forwarded-For` (SR-08) and sets `X-Forwarded-Proto https` |
| HTTPS-only export | with valid credentials, the export over HTTPS works and a plain-HTTP request from another machine is refused (403) |
| Production start | start with `SPRING_PROFILES_ACTIVE=prod`; confirm it refuses with test mode on and starts with the real keys |
| Backup and retention | the `pgdata` and `jsoncopies` volumes are in the backup; deletion after 12 months as above |

## Decisions pending review

| Decision | Chosen behaviour |
|---|---|
| D-08 | length limits (100 / 254 / 200 / 50 characters, 100 options), fields of the other type and unknown properties rejected, email pattern |
| D-09 | 403 for the HTTPS-only refusal, 500 for storage failures |
| D-10 | rate limits 120 / 20 / 10 per client and minute, 16 KiB body limit |
| D-11 | emails sent synchronously, plain text, one message per organizer address |

All other decisions (D-01 … D-07, D-12 … D-14) are resolved; see `decisions-log.md`.
