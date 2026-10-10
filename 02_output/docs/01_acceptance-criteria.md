# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

Each criterion is one observable behaviour, cites its story and rules, and is verified through a public interface (REST API, UI or email in the mail catcher). Open questions are resolved by decisions D-08 to D-14 (`decisions-log.md`, pending review).

"Valid submission" below means: every required field of the type filled, a valid email, only active options available to that type, every mandatory consent given, and a reCAPTCHA token the verifier accepts.

## Open questions

| OQ | Decision | Summary of the chosen behaviour |
|---|---|---|
| OQ-01 | D-08 | Each option has an audience (external, student; default both); only matching options are offered and accepted. |
| OQ-02 | D-09 | Consents come from the options configuration; one mandatory data-processing consent is shipped; given consents are stored with a timestamp. |
| OQ-03 | D-10 | Emails are sent after storage; an email failure keeps the registration accepted. |
| OQ-04 | D-11 | Any number of options per category, each at most once. |
| OQ-05 | D-12 | A second registration with the same email (case-insensitive) is rejected. |
| OQ-06 | D-13 | Kept until deleted by the organizers, at most 12 months after the conference (manual). |

## US-001 External participant registration

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-001-01 | Given the registration page, when the participant chooses the external participant form, then it shows exactly the fields first name, last name, email and organization / institution, the active options for external participants grouped as workshops, events, meals and other activities, and the consents, none of them preselected. | BR-01, BR-04, BR-05 |
| AC-001-02 | Given a valid external submission, when it is sent, then it is accepted and the registration with all four fields, the selected options and the given consents can be found in the organizer export. | BR-01, BR-07 |
| AC-001-03 | Given an external submission in which a required field is missing, empty or only whitespace, when it is sent, then it is rejected with a field error for that field and nothing is stored. | BR-02 |
| AC-001-04 | Given values with leading and trailing whitespace, when a valid submission is accepted, then the values are stored without that whitespace. | BR-02 |
| AC-001-05 | Given an email that is not a valid email address, when the submission is sent, then it is rejected with a field error on email and nothing is stored. | BR-03 |
| AC-001-06 | Given names and an organization containing Slovenian characters (č, š, ž, Č, Š, Ž), when the valid submission is accepted, then they appear unchanged in the stored registration, its JSON copy, both emails and the Excel export. | BR-03, NFR-01 |
| AC-001-07 | Given a submission that selects an unknown or inactive option identifier, or the same option twice, when it is sent, then it is rejected with an error on the options and nothing is stored. | BR-04, SR-04, D-11 |
| AC-001-08 | Given a submission in which a mandatory consent is not given, when it is sent, then it is rejected with an error on that consent and nothing is stored. | BR-05 |
| AC-001-09 | Given a submission without a reCAPTCHA token, or with a token the verification service rejects, when it is sent, then the backend rejects it and nothing is stored or emailed; with the production verification path (mocked endpoint) an accepted token lets a valid submission through. | SR-01, DoD-P05 |
| AC-001-10 | Given an accepted registration with an email, when another registration (of either type) is sent with the same email in different letter case, then it is rejected with a field error on email and nothing is stored. | D-12 |
| AC-001-11 | Given a text field longer than 200 characters (email longer than 254) or containing a control character such as CR or LF, when the submission is sent, then it is rejected with a field error on that field and nothing is stored. | SR-05, SB-01, D-14 |
| AC-001-12 | Given the form in the browser, when the participant submits with empty required fields, an invalid email or a mandatory consent not given, then the form shows the errors next to the fields and sends nothing; and when the backend returns field errors, each is shown next to its field. | NFR-03 |
| AC-001-13 | Given more registration requests from one client than the configured limit within the window, when another is sent, then it is refused with status 429; a request body above the configured size limit is refused with status 413; in both cases nothing is stored. | SR-03, SB-06 |

## US-002 Student registration

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-002-01 | Given the registration page, when the participant chooses the student form, then it shows exactly the fields first name, last name, email, study institution, study programme and student ID, the active options for students grouped by category, and the consents, none of them preselected. | BR-01, BR-04, BR-05 |
| AC-002-02 | Given a valid student submission, when it is sent, then it is accepted and the registration with all six fields, the selected options and the given consents can be found in the organizer export. | BR-01, BR-07 |
| AC-002-03 | Given a student submission in which study institution, study programme or student ID is missing or only whitespace, when it is sent, then it is rejected with a field error for that field and nothing is stored. | BR-01, BR-02 |
| AC-002-04 | Given an active option whose configured audience excludes students, when the options for the student form are requested, then that option is not offered; and when a student submission selects it, it is rejected and nothing is stored. | BR-04, D-08 |

## US-003 Configurable conference options

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-003-01 | Given a configuration with active and inactive options in all four categories, when the options are requested, then only the active ones are returned, each with its stable identifier, display name, category and audience, together with the configured consents and their wording. | BR-04, D-09 |
| AC-003-02 | Given the backend started with one options configuration, when the configuration file is changed (an option added, one deactivated) and the backend restarted, then the offered options change accordingly without any code change and the participant fields stay the same. | AR-04, BR-01 |
| AC-003-03 | Given an options configuration with a duplicate identifier, an unknown category or audience, or a missing display name, when the backend starts, then it refuses to start and names the problem in its log. | AR-04 |

## US-004 Registration confirmation

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-004-01 | Given a valid submission in the browser, when the backend accepts it, then the application shows a confirmation with the registration reference, the participant's name and the selected options, and the form is no longer shown. | BR-06 |
| AC-004-02 | Given a submission in the browser that the backend rejects or cannot process, when the response arrives, then no confirmation is shown, the entered values are kept and an error message without internal details is shown. | BR-06, SB-07 |

## US-005 Reliable registration storage

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-005-01 | Given a valid submission, when the success response is returned, then the registration is already in the database and a JSON copy file named after its reference exists on the configured storage path containing the registration exactly as accepted (type, fields, option identifiers, consents with timestamps, reference, submission time). | BR-07, AR-05 |
| AC-005-02 | Given that the JSON copy cannot be written, when a valid submission is sent, then the response is an error without internal details, no database row remains for it and no email is sent. | BR-07, AR-05, D-10 |
| AC-005-03 | Given that the database is unavailable, when a valid submission is sent, then the response is an error without internal details, no JSON copy remains for it and no email is sent. | BR-07, AR-05 |

## US-006 Participant email confirmation

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-006-01 | Given an accepted registration, when processing completes, then one plain-text email arrives at the participant's address from the configured sender, with the configured conference name in the subject and the participant's name, registration type, registration reference and selected option names in the body. | US-006, BR-06 |
| AC-006-02 | Given an accepted registration whose names contain markup such as `<b>x</b>`, when the participant and organizer emails are generated, then the markup appears as literal text in plain-text bodies and no header of either email contains participant input other than the participant's address as recipient. | SR-05 |
| AC-006-03 | Given that the mail server is unavailable, when a valid submission is sent, then the response still confirms the registration and it is stored with its JSON copy. | D-10, BR-07 |

## US-007 Organizer notification

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-007-01 | Given an accepted registration and configured organizer addresses, when processing completes, then every organizer address receives one email containing the submitted data and an attachment `registration-<reference>.json` whose content equals the JSON copy. | US-007, BR-07 |
| AC-007-02 | Given an accepted registration, when the organizer email is generated, then it contains only registration data (the fields of the form, options, consents, reference, submission time) and no database identifiers, stack traces, configuration or other system data. | SR-07 |

## US-008 Registration export

| AC | Given / When / Then | Rules |
|---|---|---|
| AC-008-01 | Given stored registrations of both types, when an organizer requests the export with valid organizer credentials, then the response is an Excel workbook (`.xlsx`) with one row per registration and columns for reference, submission time, type, every participant field, the selected option names and the consents given. | BR-08, US-008 |
| AC-008-02 | Given no credentials or wrong credentials, when the export is requested, then it is refused with status 401 and no registration data is returned. | BR-08, SB-02 |
| AC-008-03 | Given organizer HTTPS-only access is on, when the export is requested with valid credentials over plain HTTP from a client other than localhost, then it is refused and no registration data is returned. | SR-06 |
| AC-008-04 | Given more export requests from one client than the configured limit within the window, when another is sent, then it is refused with status 429. | SR-03, SB-06 |
| AC-008-05 | Given stored registrations, when the export is produced, then it contains only registration data and no internal database identifiers or system data. | SR-07 |
