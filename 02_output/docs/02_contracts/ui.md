# UI contract (participant ↔ frontend)

> Written in: phase 2 · Source: spec section 12, US-001, US-002, US-004, NFR-03, BR-05 · Agent: writes

The accessible names below are the public interface the component and end-to-end tests use. Text is English.

## Page states

| State | What is shown |
|---|---|
| Loading | text "Loading…" until `GET /api/form-config` answers |
| Config failed | `role="alert"`: "The registration form is currently unavailable. Please try again later." |
| Form | heading (level 1) = conference name; a `form` with accessible name "Registration form" |
| Confirmation | after 201 only (BR-06): the form is removed; a region `role="status"` with heading "Registration received", the text "Thank you, <first name> <last name>.", and a list with accessible name "Selected options" holding one item per selected option name (or the text "No options selected.") |

## Form

| Element | Accessible name | Notes |
|---|---|---|
| Registration type | radio group "Registration type" with radios "External participant" (value `EXTERNAL`) and "Student" (value `STUDENT`) | none preselected; fields and options appear only after a choice |
| Common fields | text inputs "First name", "Last name", "Email" (type `email` is not used, so the browser does not pre-empt the app's validation) | |
| External fields | "Organization / institution" | shown only for External participant |
| Student fields | "Study institution", "Study programme", "Student ID" | shown only for Student |
| Options | one `fieldset` per non-empty category, legends "Workshops", "Events", "Meals", "Other activities", in that order; one checkbox per active option available to the chosen type, labelled with the option name | all unchecked initially; options of other types are not rendered |
| Consents | one checkbox per configured consent, labelled with the consent text | never preselected (BR-05, SB-14) |
| reCAPTCHA | test mode (`recaptchaTestMode: true`): checkbox "I am not a robot (test mode)"; checked → token `test-mode-token`. Otherwise the Google v2 widget rendered with `recaptchaSiteKey` | reset after every failed submission |
| Submit | button "Register" | disabled while the request is in flight |

## Field errors (NFR-03, AC-004-02, AC-004-03)

- Field keys are those of the API (`firstName`, `lastName`, `email`, `organization`, `studyInstitution`, `studyProgramme`, `studentId`, `optionIds`, `consents.<id>`, `recaptchaToken`, `type`).
- An error for a field is shown in an element with id `<key>-error` placed directly after the field (after the options fieldsets for `optionIds`; after the consent checkbox for `consents.<id>`; after the reCAPTCHA for `recaptchaToken`). The field has `aria-invalid="true"` and `aria-describedby="<key>-error"`, so its accessible description is the message. For `optionIds` the description is set on every option checkbox.
- Client-side checks before sending (same rules as the API, spec 4.2): type chosen; required fields not blank after trimming; length limits; email pattern `^[^\s@]+@[^\s@]+\.[^\s@]+$`; every mandatory consent checked; reCAPTCHA completed. Messages: "This field is required.", "Enter a valid email address.", "Use at most <n> characters.", "This consent is required.", "Confirm that you are not a robot.", "Choose a registration type.". When any check fails, no request is sent.
- Backend field errors (400) are shown with the backend's message text, next to their fields; all entered values stay.

## General errors (AC-004-04)

Any answer other than 201 or 400-with-field-errors, or a network failure: `role="alert"` with exactly "The registration could not be completed. Please try again later."; the form and the entered values stay; nothing from the response body is shown.
