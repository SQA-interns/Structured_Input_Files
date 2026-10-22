# CR-000: Dietary requirements

> Written in: change interview · Source: requester's answers and the baseline · Agent: writes until sign-off, then read-only

- Type: change
- Baseline: example-run @ 0000000
- Requested by: example requester · Approved: 2026-01-01T00:00:00Z
- Security impact: a personal data item is added (dietary requirements), shown only in the participant's two emails and to organizers (SR-07), with the retention of OQ-06; no new endpoint, authentication or dependency. <!-- note: demonstration choice (OQ-09). Dietary information can reveal health or religion, which GDPR Art. 9 treats as special-category data needing explicit consent; this example deliberately does not address that. -->

## Intent

Participants can state their dietary requirements (for example vegetarian or an allergy) when they register, so that the organizers can plan meals. The field is optional free text on both forms and reaches the organizers in the notification email and the export.
Out of scope: validating or categorising the text, dietary options as configuration, any change to the meal options (BR-04).

## Stories

### US-009 Dietary requirements

As a participant I want to state my dietary requirements when I register so that the organizers can plan meals for me.
In scope: one optional text field on the external and the student form, stored with the registration, shown in both emails and the export. Out of scope: showing it on the confirmation page.

## Rules and data

| ID | Change | Rule |
|---|---|---|
| BR-01 | MODIFIED | There are two registration types: external participant and student. Each has the fixed fields listed under Data plus the optional dietary requirements field (BR-09); configuration never changes them. |
| BR-09 | ADDED | Dietary requirements is an optional free-text field on both registration types, at most 500 characters (OQ-07). Leading and trailing whitespace is not significant, and an empty value means none given. |

## Baseline items changed

| ID | Action | Reason |
|---|---|---|
| BR-01 | MODIFIED | The fixed field list gains one optional field. |
| AC-001-08 | MODIFIED | "Exactly the external fields" no longer holds: the form also shows the new field. |
| AC-002-07 | MODIFIED | Same for the student form. |
| E2E-04 | MODIFIED | The Slovenian-characters journey must cover the new field as well. |

## Must not change

AC-001-01..07, AC-002-01..06, AC-004-01..04, AC-005-02..03, AC-007-02, AC-008-02, E2E-01..03, E2E-05.

## Open questions

| ID | Question | Answer |
|---|---|---|
| OQ-07 | What is the maximum length of the dietary requirements text? | 500 characters; longer input is rejected. |
| OQ-08 | Is the text shown on the confirmation page? | No; only the two emails and the export show it. |
| OQ-09 | Does the text need extra protection or its own consent as special-category data? | No; it is treated as ordinary personal data under the existing consent and retention (OQ-02, OQ-06). |

The answers are the requester's decisions; the agent records no decision for them.
