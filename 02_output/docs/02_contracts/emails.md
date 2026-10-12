# Email contract (backend → SMTP)

> Written in: phase 2 · Source: spec section 8, US-006, US-007, SR-05, SR-07 · Agent: writes

Both messages are sent only after the database row and the JSON copy exist (spec section 5). A send failure is logged with the registration id only and does not change the API answer (OQ-03).

## Common rules

| Rule | Value |
|---|---|
| From | `registration.mail.from` (default `registration@konferenca.si`) |
| Recipients | exactly one `To` address per message, built from the address alone (no display name); no `Cc`, no `Bcc` |
| Subject | fixed text plus the configured conference name; never contains user input |
| Body | `text/plain; charset=UTF-8`; no HTML part; user input appears only here, as literal text |
| Headers | no header contains user input (SR-05) |

## Participant confirmation (US-006)

| Item | Value |
|---|---|
| To | the participant's email (trimmed, validated) |
| Subject | `Registration confirmed: <conference name>` |
| Body | see template |

```
Dear <firstName> <lastName>,

thank you for registering for <conference name>. Your registration has been received.

Registration type: <External participant | Student>
Selected options:
- <option name> (<Workshop | Event | Meal | Other activity>)
...                       (or the single line "- none")

Registration ID: <id>
```

## Organizer notification (US-007)

One message per address in `ORGANIZER_EMAILS` (comma separated, blanks ignored).

| Item | Value |
|---|---|
| To | one organizer address |
| Subject | `New registration: <conference name>` |
| Structure | `multipart/mixed`: the text body, then one attachment |
| Attachment | file name `registration-<id>.json`, content type `application/json`, content byte-for-byte equal to the stored JSON copy `<id>.json` (AC-007-02) |
| Body | see template; only registration data (SR-07) |

```
A new registration was received for <conference name>.

Registration ID: <id>
Registered at: <registeredAt, ISO-8601 UTC>
Type: <EXTERNAL | STUDENT>
First name: <firstName>
Last name: <lastName>
Email: <email>
Organization / institution: <organization>      (EXTERNAL)
Study institution: <studyInstitution>           (STUDENT)
Study programme: <studyProgramme>               (STUDENT)
Student ID: <studentId>                         (STUDENT)
Options:
- <option name> (<category label>)              (or "- none")
Consents:
- <consent id>: given <givenAt>

The raw registration is attached as JSON.
```
