# Contract: emails (SMTP)

> Written in: phase 2 · Source: US-006, US-007, SR-05, SR-07, D-12 · Agent: writes

Sent after the registration is stored (AR-05), one message per recipient, by Spring Mail (Jakarta Mail) to `MAIL_HOST`:`MAIL_PORT` (STARTTLS when `MAIL_TLS=true`). Bodies are `text/plain; charset=UTF-8` only, so user input cannot add markup (SR-05). User input never goes into a header: subjects are fixed text plus the configured conference name, and the only address taken from input is the validated participant email (no CR, LF or comma possible after validation). A failed send is logged as `email failed kind=<PARTICIPANT|ORGANIZER> registration=<id>` without personal data and is not retried (D-12).

## Participant confirmation (US-006)

- From: `MAIL_FROM`; To: the participant's email.
- Subject: `Registration confirmation – <CONFERENCE_NAME>`
- Body (UTF-8 text), in this order: greeting with first and last name; registration type (`External participant` or `Student`); registration id; the selected options grouped by category with display names, or `No optional activities selected.`

## Organizer notification (US-007)

- From: `MAIL_FROM`; To: each address in `ORGANIZER_EMAILS`, one message each.
- Subject: `New registration – <External participant|Student> – <CONFERENCE_NAME>`
- Body: every field of the registration with its label (the same fields as the JSON copy, SR-07), the selected options and the consent timestamp.
- Attachment: `registration-<id>.json`, `application/json`, byte-identical to the JSON copy (AC-007-02).
