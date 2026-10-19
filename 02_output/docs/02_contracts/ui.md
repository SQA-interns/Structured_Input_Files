# Registration form contract (UI)

> Written in: phase 2 · Source: AC-001-08, AC-002-07, AC-004-01..04, NFR-03, AR-01, AR-07 · Agent: writes

One page at `/`. The frontend calls only `GET /api/form-config` and `POST /api/registrations` (`openapi.json`, AR-01). UI text is English (D-08). Accessible names below are exact; tests find elements by role and accessible name.

## Elements

| Element | Role and accessible name |
|---|---|
| Registration type | radio group `Registration type` with radios `External participant` and `Student`; none selected at first; the form fields appear after a choice |
| External fields | textboxes `First name`, `Last name`, `Email`, `Organization / institution` |
| Student fields | textboxes `First name`, `Last name`, `Email`, `Study institution`, `Study programme`, `Student ID` |
| Options | one group (`fieldset`) per category that has at least one option for the chosen type, legends `Workshops`, `Events`, `Meals`, `Other activities`, in that order; one checkbox per active option available to the chosen type, named by its display name; none checked |
| Consents | one checkbox per configured consent, named by its text; never preselected |
| reCAPTCHA | test mode (`recaptcha.testMode`): a checkbox `I am not a robot (test mode)` that yields the token `test-pass` when checked; otherwise the Google reCAPTCHA v2 widget with `recaptcha.siteKey` (the only key in the frontend, AR-07) |
| Submit | button `Register` |

Only the fields of the chosen type are shown (no other textboxes).

## Validation and errors (NFR-03)

- Before sending, the page checks every required field of the type (non-blank after trimming), the email format, and the mandatory consents; with any error it sends no request.
- A field error is shown in an element with id `error-<field>` placed directly after its input, and the input has `aria-invalid="true"` and `aria-describedby="error-<field>"`. `<field>` uses the API field names (`firstName`, …, `optionIds`, `consents.<id>`, `recaptchaToken`); for `optionIds` and `consents.<id>` the described element is the options group or the consent checkbox.
- Backend field errors (400) are shown the same way; entered values remain.
- Any other failure (other status, network error) shows an element with role `alert` and the text `Registration could not be completed. Please try again later.` and no technical details; the form and values remain.

## Confirmation (AC-004-01)

After a 201 response the form is removed and an element with role `status` shows the heading `Registration received`, the participant's first and last name, and a list of the display names of the selected options.
