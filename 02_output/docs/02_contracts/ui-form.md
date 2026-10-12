# Contract: registration form (UI)

> Written in: phase 2 · Source: US-001, US-002, US-004, NFR-03, D-11, D-17 · Agent: writes

Single page served at `/`. Language: English (D-17). Tests address elements by role and accessible name, as listed here.

## Choosing the form

Two radio buttons in a group named `Registration type`: `External participant` (selected by default) and `Student`.

## Fields (labels are the accessible names)

| Form | Label | Request field |
|---|---|---|
| both | `First name` | `firstName` |
| both | `Last name` | `lastName` |
| both | `Email` | `email` |
| External participant | `Organization / institution` | `organization` |
| Student | `Study institution` | `studyInstitution` |
| Student | `Study programme` | `studyProgramme` |
| Student | `Student ID` | `studentId` |

The form shows only the fields of the selected type.

## Options

One group per category (a `fieldset`, role `group`, whose accessible name is its heading) with a heading `Workshops`, `Events`, `Meals`, `Other activities`, in that order; a group without options open to the selected type is not shown. Each option is a checkbox labelled with its display name. Only options from `GET /api/options` whose `registrationTypes` contain the selected type are shown (D-10). Switching type clears selections not open to the new type.

## Consent and anti-automation

- One checkbox labelled with the configured consent text, unchecked on load and after switching type (BR-05).
- Anti-automation as in `anti-automation.md`.

## Submit and errors

- Button `Register`. On press the form validates first (NFR-03): every required field non-blank (`This field is required.`), email format (`Enter a valid email address.`), consent (`Please give your consent.`), token. If any check fails, nothing is sent and each message is shown next to its field (element with `role="alert"` referenced by the field's `aria-describedby`).
- While the request is pending the button is disabled and labelled `Sending…`; no confirmation is shown (AC-004-03).
- 400/409 with `fieldErrors`: each message is shown next to its field; entered values stay (AC-001-16). Errors on `optionIds` and `recaptchaToken` show above the `Register` button.
- Other failures (network, 413, 429, 5xx): a form-level alert `Your registration could not be sent. Please try again.`; entered values stay (AC-004-02).

## Confirmation (US-004)

Shown only after a 201 response (BR-06), replacing the form: heading `Registration received`, the text `Thank you, <first name> <last name>. A confirmation has been sent to <email>.`, and a list of the selected option names (or `No optional activities selected.`).
