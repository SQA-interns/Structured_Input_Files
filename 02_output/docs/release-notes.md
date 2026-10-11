# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0 of the conference registration (backend `registration-backend` 0.1.0, frontend `registration-frontend` 0.1.0), branch `kyuhi/single-agent/sdd/v003`.

## Delivered

| Story | What works | Evidence |
|---|---|---|
| US-001 External participant registration | Form with the four fixed fields, options by category, unchecked consents, reCAPTCHA; server-side validation (required, trimming, email format, length, control characters, options, consents, duplicate email); rate limit and body size limit | AC-001-01..13 tests, runtime demo |
| US-002 Student registration | Form with the six fixed fields; options filtered by audience; same validation | AC-002-01..04 tests, runtime demo |
| US-003 Configurable options | Options and consents in one JSON file, validated at start, changed by editing the file and restarting | AC-003-01..03 tests |
| US-004 Confirmation | Confirmation with reference, name and activities only after acceptance; values kept and a general message on failure | AC-004-01..02 tests, e2e |
| US-005 Reliable storage | PostgreSQL row and raw JSON copy written in one unit before the success response; both on persistent volumes | AC-005-01..03 tests, runtime demo (survives `down`/`up`) |
| US-006 Participant email | Plain-text UTF-8 confirmation; an email failure does not undo the registration | AC-006-01..03 tests, Mailpit |
| US-007 Organizer notification | Registration data to every organizer address with `registration-<reference>.json` attached (identical to the copy) | AC-007-01..02 tests, Mailpit |
| US-008 Export | `.xlsx` workbook for the organizer (HTTP Basic, HTTPS-only outside localhost, rate limited); 401 without credentials | AC-008-01..05 tests, runtime demo |

Quality: 263 automated tests pass (backend 197, frontend 61, e2e 5); coverage all levels backend 97.7 % lines / 93.0 % branches, frontend 100 % / 97.5 %; mutation backend 95.4 %, frontend 84.7 % (scopes in `03_test-strategy.md`). No open Critical, High or Medium finding (`06_verification-report.md`).

## Known limitations

- Rate limits are counted in memory per backend instance; running several backend instances multiplies the effective limit, and a restart resets the counters.
- Retention (D-13): there is no deletion feature; registrations and JSON copies must be deleted by an operator (database and volume) at the latest 12 months after the conference.
- Duplicate registrations are blocked by email address (D-12); a participant who needs to change data must contact the organizers (editing is out of scope).
- reCAPTCHA in production depends on the frontend CSP allowing `www.google.com/recaptcha/` and `www.gstatic.com/recaptcha/`; the widget was only exercised with a mocked verification endpoint and in test mode.
- The frontend container forwards `/api/` to a service named `backend` through Docker's DNS (127.0.0.11); in production the external nginx routes `/api` directly, as `environments.md` describes.
- Frontend mutation testing uses Vitest 4.1.11 instead of the listed 5.0.3 because the Stryker Vitest runner does not work with Vitest 5 (D-18..D-20); `@vitest/coverage-v8` follows (4.1.11).
- Docker Engine 29.8.1 was used instead of the listed 29.8.0 (D-01, D-07).
- The Playwright end-to-end tests are not type-checked by `tsc` (no `@types/node` in the stack).
- `npm audit` reports two moderate advisories in `qs`, used only by the mutation-testing tool (D-06).
- Mutation survivors F-09 and F-11 (`06_verification-report.md`) are accepted: equivalent or UI-only.

## Must be tested manually by a human

| What | Why | How |
|---|---|---|
| One real registration with the production reCAPTCHA keys | Automated tests use test mode and a mocked verification endpoint (`environments.md`) | Set `RECAPTCHA_SITE_KEY`/`RECAPTCHA_SECRET_KEY`, `APP_RECAPTCHA_TESTMODE=false`, register through the real widget; also submit once without solving it (must be refused) |
| Email delivery through the real SMTP server | Tests use Mailpit | Configure `SPRING_MAIL_*`, `SMTP_USERNAME`, `SMTP_PASSWORD`; register; check the participant and organizer mailboxes, the JSON attachment and the Slovenian characters |
| HTTPS, redirects and `/api` routing through the external nginx with Let's Encrypt | No reverse proxy locally | Open the site over HTTPS, check HTTP→HTTPS redirect, `/api/form-config`, and that the organizer export works over HTTPS and is refused over plain HTTP from outside (403) |
| reCAPTCHA widget under the production CSP | The real widget never ran in tests | Load the form in a browser and check the console for CSP violations |
| Export workbook opened in Excel or LibreOffice | Tests read it with Apache POI | Download as organizer and open it; check columns and Slovenian characters |
| Consent wording and the conference options | D-09 wording and the shipped options are placeholders | The product owner approves `conference-options.json` for production |

## Decisions pending review

| Decision | Subject | Chosen |
|---|---|---|
| D-08 | OQ-01 options for students | per-option audience, default both |
| D-09 | OQ-02 consents | one mandatory data-processing consent from the options file; wording to approve |
| D-10 | OQ-03 storage succeeds, email fails | registration stays accepted, failure logged |
| D-11 | OQ-04 options per category | any number, each once |
| D-12 | OQ-05 same email twice | rejected (case-insensitive) |
| D-13 | OQ-06 retention | manual deletion, at most 12 months after the conference |
| D-14 | field limits | 200 characters (email 254), control characters rejected |
| D-16 | SpotBugs EI_EXPOSE_REP/REP2 excluded | filter in `spotbugs-exclude.xml` |
| D-20 | Vitest 4.1.11 within the approved 4.1.x | 4.1.11 (fixes an advisory in 4.1.10) |

Resolved with the human: D-01 (Docker 29.8.1), D-04 (CVE false positive), D-15 (frozen test AC-008-03), D-18 (Vitest 4.1.x). All other records are resolved; none lacks a resolution.
