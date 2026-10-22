# Acceptance criteria: CR-000

> Written in: change interview · Source: requester's answers · Agent: writes until sign-off, then read-only

Same rules as the baseline list: one observable behaviour per row (Given / When / Then), each citing its story and rules. Terms the baseline defines ("rejected", "valid … submission") keep their meaning here. The agent does not derive, add, merge or reword criteria.

## US-001 External participant registration

| ID        | Change   | Criterion                                                                                                                                                                                                                                                                                         | Refs                       |
| --------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| AC-001-08 | MODIFIED | Given the registration page, when the participant chooses "external participant", then the page shows exactly the external fields and the optional dietary requirements field, the active options grouped as workshops, events, meals and other activities, and every consent checkbox unchecked. | BR-01, BR-04, BR-05, BR-09 |

## US-002 Student registration

| ID        | Change   | Criterion                                                                                                                                                                                                                                                                       | Refs                              |
| --------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| AC-002-07 | MODIFIED | Given the registration page, when the participant chooses "student", then the page shows exactly the student fields and the optional dietary requirements field, and only the active options available to students, grouped by category, with every consent checkbox unchecked. | BR-01, BR-04, BR-05, BR-09, OQ-01 |

## US-009 Dietary requirements

| ID        | Change | Criterion                                                                                                                                                                                                     | Refs          |
| --------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------- |
| AC-009-01 | ADDED  | Given either form, when a valid submission has no dietary requirements (missing, empty or only whitespace), then it is accepted (HTTP 201) and the stored value is empty.                                     | BR-09         |
| AC-009-02 | ADDED  | Given either form, when a valid submission has dietary requirements with leading and trailing spaces, then it is accepted (HTTP 201) and the stored text has the spaces removed.                              | BR-02, BR-09  |
| AC-009-03 | ADDED  | Given either form, when the dietary requirements text is longer than 500 characters, then the submission is rejected (HTTP 400) with an error for that field.                                                 | BR-09, OQ-07  |
| AC-009-04 | ADDED  | Given a valid submission with dietary requirements, when it is accepted, then the database row and the JSON copy contain the text.                                                                            | BR-07, BR-09  |
| AC-009-05 | ADDED  | Given a valid submission with dietary requirements, when it is accepted, then the participant's confirmation email shows the text.                                                                            | BR-09, OQ-08  |
| AC-009-06 | ADDED  | Given a valid submission with dietary requirements, when it is accepted, then the organizer notification shows the text.                                                                                      | BR-09, US-007 |
| AC-009-07 | ADDED  | Given dietary requirements containing line breaks, header-like text or HTML markup, when the emails are sent, then they have no additional headers or recipients and the markup appears only as literal text. | SR-05         |
| AC-009-08 | ADDED  | Given a registration with dietary requirements, when the organizer exports, then the workbook row contains the text.                                                                                          | BR-08, BR-09  |

## End-to-end scenarios

Exactly one end-to-end test per scenario; no scenario is merged or split.

| ID     | Change   | Scenario                                     | Steps and observations                                                                                                                                                                                                                        | Covers                                             |
| ------ | -------- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| E2E-04 | MODIFIED | Slovenian characters survive the whole chain | Register with č, š, ž, Č, Š, Ž in the names, the institution and the dietary requirements. Each text is unchanged wherever it appears: the page confirmation (names), the database row, the JSON copy, both emails and the exported workbook. | AC-005-04, AC-008-03, AC-009-04, AC-009-08, NFR-01 |
