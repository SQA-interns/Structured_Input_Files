# Release notes

> Written in: phase 7 · Agent: writes

Version 0.1.0 of the conference registration (backend `registration-backend` 0.1.0, frontend `registration-frontend` 0.1.0).

## Delivered

- US-001, US-002: registration forms for external participants and students, client and server validation, Unicode text (č, š, ž), reCAPTCHA v2 verified on the backend.
- US-003: options (workshops, events, meals, other activities) and consents from a JSON configuration file, per-type availability, refused start-up on an invalid file.
- US-004: in-application confirmation only after acceptance; field errors next to the fields; general error without technical details.
- US-005: every accepted registration in PostgreSQL and as `<id>.json` on a persistent volume, written in one transaction.
- US-006, US-007: plain-text participant confirmation and organizer notification with the JSON copy attached; a mail failure does not lose the registration.
- US-008: Excel export for the organizer (HTTP Basic, HTTPS-only outside localhost).
- Security: rate and size limits, security headers, non-root containers, production safety check (`SPRING_PROFILES_ACTIVE=prod`).
- Quality: 35/35 acceptance criteria covered by frozen tests; backend 203 tests, line coverage 98.19 %, mutation score 90.54 %; frontend 56 tests plus 5 end-to-end, line coverage 100 %, mutation score 92.58 % (`docs/03_test-strategy.md`).

## Data retention (OQ-06)

Registrations and JSON copies are kept until the operator deletes them; nothing is deleted automatically. **Delete the database rows and the JSON copies no later than 12 months after the conference**:

```sh
docker compose --env-file ../.env exec -T postgres psql -U registration -d registration -c "TRUNCATE registration CASCADE"
docker compose --env-file ../.env exec -T backend sh -c 'rm -f /data/json-copies/*.json'
```

(in production, the same statements against the production database and JSON copy volume). Also delete exported workbooks and notification emails kept by organizers.

## Known limitations

- No administration UI: options change by editing the configuration file and restarting the backend.
- Registrations cannot be edited or cancelled; a second registration with the same email is stored separately (OQ-05).
- The rate limiter keeps its counters in memory per backend instance; with several instances each counts separately.
- Mailpit (local) keeps emails in memory; they are gone after `docker compose down`.
- The backend container image is built from a jar built on the host (no JDK build image is pinned in `tech-stack.md`).
- `/api` responses through the frontend container carry two Content-Security-Policy headers (nginx and backend); browsers apply both (F-06).
- End-to-end tests are not part of coverage or mutation measurement (D-14).

## Must be tested manually by a human

| # | What | How | Why not automated |
|---|---|---|---|
| 1 | Google reCAPTCHA v2 with production keys | deploy with `SPRING_PROFILES_ACTIVE=prod`, real `RECAPTCHA_SITE_KEY`/`RECAPTCHA_SECRET_KEY`; submit one registration through the widget; submit one with an expired token (wait 2 minutes) and see the field error | live Google service (`environments.md`) |
| 2 | Email delivery through the production SMTP server | one registration with a real mailbox; check the participant email and the organizer email with `registration-<id>.json` attached, and that Slovenian characters display correctly | external SMTP server |
| 3 | HTTPS, redirects and `/api` routing at the external nginx | HTTP redirects to HTTPS; `/` serves the frontend, `/api/form-config` reaches the backend; the proxy sets `Host` itself and `X-Forwarded-For`/`X-Forwarded-Proto` (SR-08) | external proxy and Let's Encrypt |
| 4 | Organizer export in production | over HTTPS with organizer credentials: workbook downloads; over plain HTTP from another machine: refused (403) | needs the production proxy |
| 5 | Production safety check | start the backend with profile `prod` and `RECAPTCHA_TEST_MODE=true` (or empty keys, or `ORGANIZER_HTTPS_ONLY=false`): it must refuse to start | production configuration |
| 6 | Persistence on the production host | recreate the containers; registrations and JSON copies remain (NFR-02) | production volumes |
| 7 | Browser check of the form | the form in current Firefox and Safari (automated tests use Chromium only), keyboard-only and screen-reader use | other browsers and assistive technology |

## Decisions pending review

| Decision | Topic | Applied |
|---|---|---|
| D-01 | host JDK is Oracle 21.0.11, not Temurin (runtime image is Temurin) | PA-05 |
| D-10 | HTTPS-only refusal answers 403 before credentials are checked | option 1 |
| D-13 | PMD `GuardLogStatement` and SpotBugs `EI_EXPOSE_REP*`, `CT_CONSTRUCTOR_THROW` excluded | option 1 |

All other decisions (D-02 … D-09, D-11, D-12, D-14, D-15) are resolved; see `docs/decisions-log.md`.
