# Contract: anti-automation (Google reCAPTCHA v2)

> Written in: phase 2 · Source: SR-01, SR-02, AR-07, `environments.md` · Agent: writes

## Frontend

- `GET /api/settings` → `recaptcha.testMode`, `recaptcha.siteKey`. The site key is the frontend's only key and comes from backend configuration (AR-07).
- Live mode: the form loads `https://www.google.com/recaptcha/api.js` and renders the checkbox widget with the site key; the token from the widget is sent as `recaptchaToken`.
- Test mode: no Google script is loaded; the form shows a checkbox labelled `I am not a robot (test mode)`; checking it sets the token to `test-mode-pass`.
- Without a token the form does not submit and shows `Please confirm you are not a robot.`

## Backend

- Every `POST /api/registrations` is verified on the server (SR-01); a missing, blank or rejected token gives 400 with a field error on `recaptchaToken`.
- Live mode: `POST <RECAPTCHA_VERIFY_URL>` (default `https://www.google.com/recaptcha/api/siteverify`), form fields `secret`, `response`, `remoteip`; accepted only when the JSON answer has `"success": true`. A timeout (5 s) or error answer counts as rejected. The URL is configurable so tests run the production path against a mocked endpoint (DoD-P05).
- Test mode (`RECAPTCHA_TEST_MODE=true`, deterministic): accepts exactly the token `test-mode-pass`, rejects every other token, makes no network call.
- Startup (SR-02): test mode is off unless configured. With test mode off, empty `RECAPTCHA_SITE_KEY` or `RECAPTCHA_SECRET_KEY` stops the startup. With the `prod` profile active, test mode on stops the startup.
