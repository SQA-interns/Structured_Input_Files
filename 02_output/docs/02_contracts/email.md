# Email contract (SMTP)

> Written in: phase 2 · Source: US-006, US-007, SR-05, SR-07, OQ-03 · Agent: writes

Both emails are sent only after the registration transaction committed (database row and JSON copy exist), asynchronously, at most 10 seconds after the 201 response in the test and local environments. A send failure is logged with the registration id only and never changes the response (OQ-03, AC-006-03).

Common to both: `From` = `MAIL_FROM`; `Content-Type: text/plain; charset=UTF-8` body (no HTML part, so markup stays literal text, SR-05); headers are built only from configuration and validated addresses, never from free text (no header injection); the only recipients are the ones named below.

## Participant confirmation (AC-006-01, AC-006-02)

| Item | Value |
|---|---|
| To | the participant's email (exactly one recipient, no Cc/Bcc) |
| Subject | `Registration confirmation - <conferenceName>` |
| Body | contains the conference name, the participant's first and last name, and the display name of every selected option (or `none`) |

## Organizer notification (AC-007-01, AC-007-02)

| Item | Value |
|---|---|
| To | every address in `ORGANIZER_EMAILS` (comma separated), one message per address |
| Subject | `New registration - <conferenceName>` |
| Body | registration id, type, every submitted field of the type, the selected options and the consents with the time given; no internal or system data (SR-07) |
| Attachment | `registration-<id>.json`, `application/json`, bytes equal to the stored JSON copy |
