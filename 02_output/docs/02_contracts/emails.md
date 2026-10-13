# Email contract (SMTP)

> Written in: phase 2 · Source: US-006, US-007, SR-05, SR-07, OQ-03 · Agent: writes

Both emails are sent after the database transaction (with the JSON copy) has committed, after the 201 answer has been decided, by the backend through the configured SMTP server (`SMTP_HOST`, `SMTP_PORT`, `SMTP_STARTTLS`, optional `SMTP_USERNAME`/`SMTP_PASSWORD`). Connection and read timeouts are 5 s. A failure of either email is logged as `Email <kind> for registration <id> failed` with no personal data and does not change the API answer (OQ-03).

## Common rules (SR-05)

- `From`: `MAIL_FROM` (setting, default `registration@konferenca.local`).
- Content: `text/plain; charset=UTF-8` only, no HTML part. User input appears only in the body, so markup is literal text.
- Headers never contain user input: the subject is built from `CONFERENCE_NAME` only; recipients are addresses that passed the email rule (no whitespace, so no CR/LF) or configured organizer addresses; no display names.
- Exactly one recipient per message in `To`; no `Cc` or `Bcc`.

## Participant confirmation (US-006, AC-006-01, AC-006-02)

| Item | Value |
|---|---|
| To | the participant's email (trimmed) |
| Subject | `Registration confirmation – <CONFERENCE_NAME>` |
| Body | lines, in order: `Dear <firstName> <lastName>,` · blank · `your registration for <CONFERENCE_NAME> has been received.` · blank · `Registration ID: <id>` · `Registration type: External participant` or `Student` · blank · `Selected options:` · one line `- <option name> (<Workshop|Event|Meal|Other activity>)` per selected option, or `- none` · blank · `Kind regards,` · `<CONFERENCE_NAME> organizers` |

## Organizer notification (US-007, AC-007-01, AC-007-02, SR-07)

One message per address in `ORGANIZER_EMAILS` (comma separated, each trimmed and validated at start-up; an invalid address stops start-up).

| Item | Value |
|---|---|
| To | one organizer address |
| Subject | `New registration – <CONFERENCE_NAME>` |
| Body | `A new registration was received.` · blank · then one `Label: value` line each for `Registration ID`, `Registration type`, `Registered at` (ISO-8601 UTC), `First name`, `Last name`, `Email`, and either `Organization / institution` or `Study institution`, `Study programme`, `Student ID` · blank · `Selected options:` with lines as in the participant email · blank · `Consents:` with one line `- <consent id>: given at <ISO-8601 UTC>` per consent |
| Attachment | `registration-<id>.json`, content type `application/json`, the exact bytes of the stored JSON copy (`registration-copy.schema.json`) |

Only registration data appears (SR-07): no internal ids other than the registration id, no configuration values, no server information.
