# Contract: anti-automation (frontend, backend, Google reCAPTCHA v2)

> Written in: phase 2 · Source: specification section 9, SR-01, SR-02, DoD-P05 · Agent: writes

## Frontend widget

- `GET /api/form-config` → `captcha.mode`, `captcha.siteKey`.
- `recaptcha` mode: load `https://www.google.com/recaptcha/api.js?render=explicit` and render the v2 checkbox with `siteKey` into the element with id `recaptcha`; the widget's token is sent as `captchaToken`. Without a token the client shows `Please confirm that you are not a robot.` and sends nothing; the widget is reset after a rejected submission.
- `test` mode: no script is loaded; the page shows `Test mode: captcha disabled` and sends `captchaToken: "TEST-PASS"`.

## Backend verification (production code path)

Request: `POST <APP_RECAPTCHA_VERIFY_URL>`, `Content-Type: application/x-www-form-urlencoded`, fields `secret=<RECAPTCHA_SECRET_KEY>`, `response=<captchaToken>`, `remoteip=<client address>`. Timeout 5 s.

| Verifier answer | Result |
|---|---|
| 200, JSON with `"success": true` | accepted |
| 200, JSON with `"success": false` (any `error-codes`) | rejected → API 400, field `captchaToken` |
| non-200, unparsable body, timeout, connection error | unavailable → API 503, nothing stored |

## Test mode (test and local only)

`APP_RECAPTCHA_TEST_MODE=true`: token `TEST-PASS` accepted, every other token rejected (400); no network call. With `APP_ENVIRONMENT=production` the backend refuses to start with test mode on or with an empty site or secret key (SR-02).
