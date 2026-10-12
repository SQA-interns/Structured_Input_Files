# Acceptance criteria

> Written in: phase 1 · Source: `project/01_requirements/*` · Agent: writes (frozen after phase 3)

One observable behaviour per criterion, written Given / When / Then against a public interface (form or REST API). "Accepted" means the backend answers with success; "rejected" means it answers with an error, stores nothing, writes no JSON copy and sends no email. Decisions for the open questions: OQ-01 → D-10, OQ-02 → D-11, OQ-03 → D-12, OQ-04 → D-13, OQ-05 → D-14, OQ-06 → D-15 (no behaviour to test; retention is a manual task).

A "valid external registration" has non-empty first name, last name, email in valid format and organization / institution, only active options open to external participants, the mandatory consent given and a valid anti-automation token. A "valid student registration" is the same with study institution, study programme and student ID instead of organization, and options open to students.

## US-001 External participant registration

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-001-01 | BR-01, BR-04 | active options are configured | the participant opens the external form | it shows exactly the fields first name, last name, email, organization / institution, and the active options open to external participants grouped as workshops, events, meals and other activities |
| AC-001-02 | BR-05 | the external form is open | the participant looks at the consent | the mandatory consent with its configured wording is shown unchecked |
| AC-001-03 | BR-01, BR-04, BR-07 | a valid external registration | it is submitted | it is accepted and a registration id is returned |
| AC-001-04 | BR-02 | an external registration with one required field empty or only whitespace | it is submitted | it is rejected with an error on that field |
| AC-001-05 | BR-02 | an external registration whose fields have leading and trailing spaces | it is submitted | it is accepted and the stored values have no leading or trailing whitespace |
| AC-001-06 | BR-03 | an external registration with an email in invalid format | it is submitted | it is rejected with an error on the email field |
| AC-001-07 | BR-03 | an external registration with č, š and ž in its name and organization | it is submitted | it is accepted and the text is stored unchanged |
| AC-001-08 | BR-04 | an external registration selecting an unknown option id | it is submitted | it is rejected with an error on the options |
| AC-001-09 | BR-04 | an external registration selecting an inactive option | it is submitted | it is rejected with an error on the options |
| AC-001-10 | BR-04, D-13 | an external registration selecting the same option twice | it is submitted | it is rejected with an error on the options |
| AC-001-11 | BR-04, D-13 | a valid external registration with several options in one category, or none at all | it is submitted | it is accepted |
| AC-001-12 | BR-05 | an external registration without the mandatory consent | it is submitted | it is rejected with an error on the consent |
| AC-001-13 | scope (anti-automation) | an external registration without an anti-automation token, or with a token the verification rejects | it is submitted | it is rejected |
| AC-001-14 | D-14 | a registration already exists for an email | an external registration with the same email, differing only in case or surrounding spaces, is submitted | it is rejected with an error on the email field |
| AC-001-15 | BR-02, BR-03 | the external form with a required field empty or an invalid email | the participant presses submit | the form shows an error next to that field and sends nothing |
| AC-001-16 | BR-02 | the backend rejects a submission with field errors | the response arrives | the form shows each error next to its field and keeps the entered values |

## US-002 Student registration

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-002-01 | BR-01, BR-04, D-10 | active options are configured, some of them not open to students | the student opens the student form | it shows exactly the fields first name, last name, email, study institution, study programme, student ID, and only the active options open to students, grouped by category |
| AC-002-02 | BR-05 | the student form is open | the student looks at the consent | the mandatory consent is shown unchecked |
| AC-002-03 | BR-01, BR-07 | a valid student registration | it is submitted | it is accepted and a registration id is returned |
| AC-002-04 | BR-02 | a student registration with study institution, study programme or student ID empty or only whitespace | it is submitted | it is rejected with an error on that field |
| AC-002-05 | BR-03 | a student registration with an email in invalid format | it is submitted | it is rejected with an error on the email field |
| AC-002-06 | BR-04, D-10 | a student registration selecting an active option not open to students | it is submitted | it is rejected with an error on the options |
| AC-002-07 | BR-05 | a student registration without the mandatory consent | it is submitted | it is rejected with an error on the consent |
| AC-002-08 | scope (anti-automation) | a student registration with a token the verification rejects | it is submitted | it is rejected |
| AC-002-09 | BR-02, BR-03 | the student form with a required field empty | the student presses submit | the form shows an error next to that field and sends nothing |

## US-003 Configurable conference options

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-003-01 | BR-04 | a configuration with active and inactive options | the options are requested from the API | only active options are returned, each with its stable identifier, display name, category and the registration types it is open to |
| AC-003-02 | BR-01, BR-04 | the backend runs with one option configuration | an option is added, another deactivated and the backend restarted with the changed configuration only | the API and forms offer the new option and no longer the deactivated one, and the participant fields are unchanged |
| AC-003-03 | BR-04 | an option configuration with an unknown category or a duplicated identifier | the backend starts | it refuses to start and names the faulty entry |

## US-004 Registration confirmation

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-004-01 | BR-06 | a valid registration in the form | the backend accepts it | the application shows a confirmation that the registration was received, with the selected options |
| AC-004-02 | BR-06 | a submitted registration | the backend rejects it or cannot be reached | no confirmation is shown; an error message is shown and the entered values are kept |
| AC-004-03 | BR-06 | a submitted registration | the backend has not answered yet | no confirmation is shown and the form cannot be submitted a second time |

## US-005 Reliable registration storage

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-005-01 | BR-07 | a valid registration | it is accepted | the database holds it with registration type, every field, the selected option ids, the consent with its timestamp and the submission time |
| AC-005-02 | BR-07 | a valid registration | it is accepted | a JSON file named after its registration id exists in the JSON copy directory and holds the registration as accepted |
| AC-005-03 | BR-06, BR-07 | the JSON copy cannot be written | a valid registration is submitted | it is rejected with a server error, no database row remains and no email is sent |
| AC-005-04 | BR-06, BR-07 | the database is unavailable | a valid registration is submitted | it is rejected with a server error, no JSON copy remains and no email is sent |

## US-006 Participant email confirmation

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-006-01 | — | a valid registration | it is accepted | one email goes to the participant's address with their name, registration type and selected options |
| AC-006-02 | BR-03 | a registration with č, š and ž in the name | it is accepted | the participant email shows the characters unchanged |
| AC-006-03 | — | a registration whose text fields contain HTML markup and line breaks | it is accepted | the email shows the markup as plain text and has no headers other than those the system sets |
| AC-006-04 | D-12 | the mail server is unavailable | a valid registration is submitted | it is accepted and stored, with its JSON copy |
| AC-006-05 | — | an invalid registration | it is rejected | no email is sent |

## US-007 Organizer notification

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-007-01 | — | organizer addresses are configured | a registration is accepted | one email goes to each organizer address with the submitted registration data |
| AC-007-02 | BR-07 | a registration is accepted | the organizer email is sent | it has a JSON attachment whose content equals the registration's JSON copy |
| AC-007-03 | — | a registration is accepted | the organizer email is sent | it contains only registration data: no internal fields, stack traces, configuration or other registrations |

## US-008 Registration export

| ID | Rules | Given | When | Then |
|---|---|---|---|---|
| AC-008-01 | BR-08 | external and student registrations exist | the organizer requests the export with valid organizer credentials | an Excel workbook is returned with one row per registration and columns for type, every field, selected options, consent with timestamp and submission time |
| AC-008-02 | BR-03 | a registration with č, š and ž | the organizer exports | the workbook shows the characters unchanged |
| AC-008-03 | BR-08 | — | the export is requested without credentials | it is refused with 401 and no registration data |
| AC-008-04 | BR-08 | — | the export is requested with a wrong username or password | it is refused with 401 and no registration data |
| AC-008-05 | — | a registration whose text starts with `=`, `+`, `-` or `@` | the organizer exports | the cell holds the text as entered and is not a formula |
| AC-008-06 | — | registrations exist | the organizer exports | the workbook holds only registration data: no internal ids other than the registration id, no other system data |

## Traceability

| Story | Criteria | Count |
|---|---|---|
| US-001 | AC-001-01 … AC-001-16 | 16 |
| US-002 | AC-002-01 … AC-002-09 | 9 |
| US-003 | AC-003-01 … AC-003-03 | 3 |
| US-004 | AC-004-01 … AC-004-03 | 3 |
| US-005 | AC-005-01 … AC-005-04 | 4 |
| US-006 | AC-006-01 … AC-006-05 | 5 |
| US-007 | AC-007-01 … AC-007-03 | 3 |
| US-008 | AC-008-01 … AC-008-06 | 6 |
| Total | | 49 |

| Business rule | Criteria |
|---|---|
| BR-01 | AC-001-01, AC-001-03, AC-002-01, AC-002-03, AC-003-02 |
| BR-02 | AC-001-04, AC-001-05, AC-001-15, AC-001-16, AC-002-04, AC-002-09 |
| BR-03 | AC-001-06, AC-001-07, AC-001-15, AC-002-05, AC-002-09, AC-006-02, AC-008-02 |
| BR-04 | AC-001-01, AC-001-08 … AC-001-11, AC-002-01, AC-002-06, AC-003-01 … AC-003-03 |
| BR-05 | AC-001-02, AC-001-12, AC-002-02, AC-002-07 |
| BR-06 | AC-004-01 … AC-004-03, AC-005-03, AC-005-04 |
| BR-07 | AC-001-03, AC-002-03, AC-005-01 … AC-005-04, AC-007-02 |
| BR-08 | AC-008-01, AC-008-03, AC-008-04 |

| Open question | Decision | Criteria |
|---|---|---|
| OQ-01 | D-10 | AC-002-01, AC-002-06 |
| OQ-02 | D-11 | AC-001-02, AC-001-12, AC-002-02, AC-002-07 |
| OQ-03 | D-12 | AC-006-04 |
| OQ-04 | D-13 | AC-001-10, AC-001-11 |
| OQ-05 | D-14 | AC-001-14 |
| OQ-06 | D-15 | none (manual retention task, release notes) |
