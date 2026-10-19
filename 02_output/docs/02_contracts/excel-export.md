# Excel export contract

> Written in: phase 2 · Source: US-008, BR-08, SR-07, NFR-01 · Agent: writes

`GET /api/registrations/export` (`openapi.json`) returns an `.xlsx` workbook (Office Open XML) with one sheet named `Registrations`.

- Row 1 is the header; then one row per stored registration, ordered by registration time ascending.
- Every cell is a string cell (no formulas; text starting with `=`, `+`, `-` or `@` is stored as plain text, never evaluated).
- Only registration data, no internal fields (SR-07).

| Column | Header | Content |
|---|---|---|
| A | Registration ID | UUID |
| B | Type | `EXTERNAL` or `STUDENT` |
| C | Registered at | ISO-8601 UTC instant, as in the JSON copy |
| D | First name | |
| E | Last name | |
| F | Email | |
| G | Organization / institution | empty for students |
| H | Study institution | empty for external participants |
| I | Study programme | empty for external participants |
| J | Student ID | empty for external participants |
| K | Options | selected option ids, in configuration order, joined by `, ` (empty when none) |
| L | Consents | `<consentId> (<givenAt>)` per consent, joined by `, ` |
