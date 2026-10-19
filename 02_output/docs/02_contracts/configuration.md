# Configuration contract (backend environment)

> Written in: phase 2 · Source: `project/00_setup/environments.md`, `secrets.env.example`, ES-01, SR-02, SR-03, SR-06 · Agent: writes

Spring relaxed binding: each variable may also be given as the property in the second column. A setting without a default must be provided; the backend refuses to start without it.

| Variable | Property | Default | Notes |
|---|---|---|---|
| `APP_ENVIRONMENT` | `app.environment` | `production` | `production`, `local` or `test`. Production refuses to start when reCAPTCHA test mode is on or a reCAPTCHA key is empty (SR-02) |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` | `spring.datasource.*` | none | |
| `POSTGRES_PASSWORD` | `spring.datasource.password` | none | secret |
| `MAIL_HOST`, `MAIL_PORT` | `spring.mail.host`, `spring.mail.port` | none | |
| `MAIL_TLS` | `app.mail.tls` | `true` | STARTTLS required when true; local and test set false (Mailpit) |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | `spring.mail.username`, `spring.mail.password` | empty | secret; production only |
| `MAIL_FROM` | `app.mail.from` | `registration@konferenca.si` | |
| `ORGANIZER_EMAILS` | `app.organizer.emails` | none | comma separated |
| `ORGANIZER_USERNAME`, `ORGANIZER_PASSWORD` | `app.organizer.username`, `app.organizer.password` | none | secret; password hashed with BCrypt at startup, the plain value is not kept |
| `ORGANIZER_HTTPS_ONLY` | `app.organizer.https-only` | `true` | may be false only on local plain HTTP (SR-06) |
| `CONFERENCE_CONFIG_FILE` | `app.conference.config-file` | none | path of the JSON file (`conference-config.schema.json`) |
| `JSON_COPY_DIR` | `app.json-copy.dir` | `/data/registrations` | persistent volume |
| `RECAPTCHA_TEST_MODE` | `app.recaptcha.test-mode` | `false` | test mode accepts only the token `test-pass` and makes no network call |
| `RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET_KEY` | `app.recaptcha.site-key`, `app.recaptcha.secret-key` | empty | secret key never leaves the backend |
| `RECAPTCHA_VERIFY_URL` | `app.recaptcha.verify-url` | `https://www.google.com/recaptcha/api/siteverify` | tests point it at a mock endpoint (DoD-P05) |
| `CORS_ALLOWED_ORIGIN` | `app.cors.allowed-origin` | empty (no CORS) | local only |
| `RATE_LIMIT_FORM_CONFIG_PER_MINUTE` | `app.rate-limit.form-config-per-minute` | `60` | per client address (SR-03) |
| `RATE_LIMIT_REGISTRATION_PER_MINUTE` | `app.rate-limit.registration-per-minute` | `10` | |
| `RATE_LIMIT_EXPORT_PER_MINUTE` | `app.rate-limit.export-per-minute` | `10` | counts failed logins too (SB-06) |
| `MAX_REQUEST_BYTES` | `app.max-request-bytes` | `16384` | larger bodies get 413 |

The client address is the TCP peer, or the `X-Forwarded-For` value set by a trusted proxy (Tomcat RemoteIpValve, internal address ranges only; `server.forward-headers-strategy=native`).

Health and readiness (ES-09, NFR-04): `/actuator/health/liveness` and `/actuator/health/readiness` on the backend port; not under `/api`, so the reverse proxy does not publish them.
