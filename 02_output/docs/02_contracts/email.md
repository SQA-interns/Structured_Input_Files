# Contract: emails (backend → SMTP)

> Written in: phase 2 · Source: specification sections 6 and 7, US-006, US-007, SR-05, SR-07 · Agent: writes

Both messages: `From` = `APP_MAIL_FROM`; plain text, `charset=UTF-8` (the organizer message is `multipart/mixed` with a `text/plain` part and the attachment); no HTML part. User input never appears in a header. Sent after the registration is committed; a failure is logged with the registration id only.

## Participant confirmation (US-006)

- To: the registration's email address (exactly one recipient; no Cc/Bcc).
- Subject: `Registration confirmation – <conference name>`
- Body (plain text):

```
Dear <first name> <last name>,

your registration for <conference name> has been received.

Registration ID: <id>
Registration type: External participant | Student
Selected options:
- <category label>: <option name>
(or "- none")

Kind regards,
<conference name> organizers
```

## Organizer notification (US-007)

- To: every address in `ORGANIZER_EMAILS` (one message, all addresses in `To`).
- Subject: `New registration – <conference name>`
- Body (plain text): registration id, type, registered at (UTC ISO-8601), each participant field with its label (`First name`, `Last name`, `Email`, `Organization / institution` or `Study institution`, `Study programme`, `Student ID`), selected options with category labels, consents with time.
- Attachment: file name `registration-<id>.json`, content type `application/json`, content = the bytes of the stored JSON copy (`registration-copy.schema.json`).

Category labels: `WORKSHOP` → Workshop, `EVENT` → Event, `MEAL` → Meal, `OTHER` → Other activity.
