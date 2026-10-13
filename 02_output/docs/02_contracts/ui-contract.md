# UI contract (registration page)

> Written in: phase 2 · Source: US-001, US-002, US-004, BR-01, BR-04 … BR-06, NFR-03, AR-01, AR-07 · Agent: writes

The frontend's public interface for acceptance (component) tests and end-to-end tests. Texts and accessible names below are exact (case-sensitive); tests select elements by role, label and the `data-testid` values listed here, never by CSS class or internal structure.

## Module and network

- `src/App.tsx` exports a named React component `App` with no props; `src/main.tsx` only mounts it.
- The app calls the global `fetch` with relative string URLs only (AR-01):
  - on mount: `fetch("/api/form-config")` → `FormConfig` (`openapi.yaml`);
  - on submit: `fetch("/api/registrations", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(RegistrationRequest) })`.
- The request body contains `type`, `firstName`, `lastName`, `email`, the type's own fields only, `optionIds` (selected ids in configuration order), `consents` (every configured consent id → checkbox state) and `recaptchaToken`.
- No secret is bundled (AR-07); the reCAPTCHA site key comes from `FormConfig.captcha.siteKey`.

## States

| State | What is shown |
|---|---|
| Loading | text `Loading…` |
| Configuration failed (non-200 or network error) | an element with role `alert` and text `The registration form is currently unavailable. Please try again later.`; no form |
| Ready, no type chosen | heading level 1 `Conference registration`; the conference name as a paragraph; the type choice; no field, option, consent or submit button |
| Ready, type chosen | the type choice, the type's fields, options, consents, anti-automation control, button `Register` |
| Submitting | button `Register` disabled |
| Accepted (201) | heading level 2 `Registration received`; text `Thank you, <firstName> <lastName>.` (values from the response); a list (role `list`, accessible name `Selected options`) with one item per selected option name, or the text `No options selected.`; no form, no `Register` button |
| Rejected (400) | the form with every entered value kept, and each field error next to its field (below) |
| Other failure (any other status, network error, invalid response) | the form with values kept and an element with role `alert`, text `Registration could not be completed. Please try again later.`; no technical detail |

## Type choice

A `radiogroup` (fieldset) with legend `Registration type` and two radios, none checked initially: `External participant` (value `EXTERNAL`), `Student` (value `STUDENT`). Switching type keeps the shared fields' values.

## Fields (labels are the accessible names)

| API field | Label | Shown for |
|---|---|---|
| firstName | `First name` | both |
| lastName | `Last name` | both |
| email | `Email` (input type `email`, `noValidate` on the form) | both |
| organization | `Organization / institution` | EXTERNAL |
| studyInstitution | `Study institution` | STUDENT |
| studyProgramme | `Study programme` | STUDENT |
| studentId | `Student ID` | STUDENT |

No other text input exists.

## Options

One `group` (fieldset) per category that has at least one active option available to the chosen type, in this order and with these legends: `Workshops` (WORKSHOP), `Events` (EVENT), `Meals` (MEAL), `Other activities` (OTHER). Each option is a checkbox whose label is the option's `name`, unchecked initially, in configuration order. Options not available to the chosen type are not rendered (OQ-01). The options group container has `data-testid="options"`.

## Consents

A `group` (fieldset) with legend `Consents`; one checkbox per configured consent, labelled with its `text`, never preselected (BR-05).

## Anti-automation

- `captcha.mode = "test"`: a checkbox labelled `I am not a robot (test mode)`; checked → token `test-pass`, unchecked → no token.
- `captcha.mode = "recaptcha"`: the Google reCAPTCHA v2 widget rendered explicitly into `<div data-testid="recaptcha">` with the site key; the token is the widget's response; after a rejected submission the widget is reset.

## Field errors (NFR-03)

Each error is rendered directly after its field or group as an element with `data-testid="error-<field>"`, and the input (or the group's fieldset) gets `aria-invalid="true"` and `aria-describedby` pointing at it. `<field>` is the API field name: `firstName`, `lastName`, `email`, `organization`, `studyInstitution`, `studyProgramme`, `studentId`, `options`, `consents.<consentId>`, `recaptchaToken`. Backend errors for `type` or `body` are shown in the general `alert` with the text of the "other failure" state.

Client-side validation runs on submit before any request (AC-004-03); if any rule fails, no request is sent:

| Rule | Message |
|---|---|
| required field empty or only whitespace | `This field is required.` |
| email does not match `^[^\s@]+@[^\s@]+\.[^\s@]+$` after trimming | `Enter a valid email address.` |
| mandatory consent unchecked | `This consent is required.` |
| no anti-automation token | `Please confirm that you are not a robot.` |

Backend field errors (400) are shown with the backend's message text, replacing client errors for the same field. Errors are cleared on the next submit.
