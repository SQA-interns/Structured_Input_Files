# Contract: registration form (participant ↔ frontend)

> Written in: phase 2 · Source: specification section 12, US-001, US-002, US-004, NFR-03, BR-05, BR-06 · Agent: writes

The acceptance and end-to-end tests find elements only by the roles, accessible names and texts below.

## Page

- Heading level 1: `Conference registration – <conference name>` (conference name from `/api/form-config`). While loading: text `Loading…`. If the configuration cannot be loaded: role `alert` with text `The registration form is currently unavailable. Please try again later.`
- Radio group (role `radiogroup`, name `Registration type`) with radios `External participant` (checked by default) and `Student`.

## Fields (each an `<input>` with a visible `<label>`; accessible name = label)

| Field | Label | Shown for | Input type |
|---|---|---|---|
| firstName | `First name` | both | text |
| lastName | `Last name` | both | text |
| email | `Email` | both | email |
| organization | `Organization / institution` | external | text |
| studyInstitution | `Study institution` | student | text |
| studyProgramme | `Study programme` | student | text |
| studentId | `Student ID` | student | text |

Fields of the other type are not rendered. Values typed into the shared fields are kept when the type changes. The form has `noValidate`, so only the rules below apply.

## Options

One `fieldset` (role `group`) per category that has at least one option available to the selected type, with legend `Workshops`, `Events`, `Meals`, `Other activities` (in this order). Each option is a checkbox whose accessible name is the option name. None is checked initially. Options not available to the selected type are not rendered; a checked option that becomes unavailable on a type change is unchecked.

## Consents

A `fieldset` with legend `Consents`; each consent is a checkbox whose accessible name is the consent text; mandatory consents have `aria-required="true"`. Never checked initially (BR-05).

## Captcha

See `recaptcha.md`: element with id `recaptcha` in `recaptcha` mode; text `Test mode: captcha disabled` in `test` mode.

## Submit

Button `Register`. While the request runs the button is disabled.

## Validation and errors (NFR-03)

- Client-side, on submit, before any request: empty required field (after trimming) → `This field is required.`; invalid email (`^[^\s@]+@[^\s@]+\.[^\s@]+$` after trimming) → `Enter a valid email address.`; unticked mandatory consent → `This consent is required.`. No request is sent while any client error exists (AC-004-03).
- A field error is rendered as text next to its field; the input gets `aria-invalid="true"` and its accessible description (`aria-describedby`) is the error message. Server field errors (`fieldErrors[].field`) map to the same fields; `consents.<id>` to that consent's checkbox; `optionIds` to a text under the options; `captchaToken` to a text under the captcha; any other field to the general error.
- Server rejection (400): messages next to their fields; entered values stay (AC-004-02).
- Other failure (network, 5xx, 413): role `alert` with text `Registration could not be completed. Please try again later.`, no confirmation (AC-004-04). 429: role `alert` with text `Too many attempts. Please wait a minute and try again.`

## Confirmation (BR-06, AC-004-01)

Shown only after a 201 response, replacing the form: heading level 2 `Registration received`, text `Thank you, <first name> <last name>. Your registration has been received.`, a list of the selected option names (or `No options selected.`), and `Registration ID: <id>`.
