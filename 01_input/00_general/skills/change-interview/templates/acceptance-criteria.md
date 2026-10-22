# Acceptance criteria: CR-<nnn>

> Written in: change interview · Source: requester's answers · Agent: writes until sign-off, then read-only

Same rules as the baseline list: one observable behaviour per row (Given / When / Then), each citing its story and rules. Terms the baseline defines ("rejected", "valid … submission") keep their meaning here. The agent does not derive, add, merge or reword criteria.

## US-<nnn> <title>

<!-- ask: which observable behaviours prove the story? One behaviour per row, with the status or visible result. Look up, do not ask: status codes, error shapes and terms in the baseline list. Existing stories keep their baseline heading and hold only their MODIFIED or REMOVED rows. Numbering continues after the highest AC of the story. -->

| ID | Change | Criterion | Refs |
|---|---|---|---|
| AC-<nnn>-<nn> | <ADDED / MODIFIED / REMOVED> | Given <state>, when <action>, then <observable result>. | <BR, SR, NFR, OQ ids> |

## End-to-end scenarios

<!-- delete this section if none. ask: user-visible journeys the existing scenarios do not cover, or existing scenarios the change alters. Detailed cases belong in the criteria above; a scenario checks the journey once. -->

Exactly one end-to-end test per scenario; no scenario is merged or split.

| ID | Change | Scenario | Steps and observations | Covers |
|---|---|---|---|---|
| E2E-<nn> | <ADDED / MODIFIED / REMOVED> | <title> | <steps and what is observed> | <AC, NFR ids> |
