# CR-<nnn>: <title>

> Written in: change interview · Source: requester's answers and the baseline · Agent: writes until sign-off, then read-only

- Type: <add | change | remove> <!-- ask: one only; a mixed request becomes several CRs -->
- Baseline: <branch> @ <sha> <!-- fill: from git, do not ask -->
- Requested by: <name> · Approved: pending <!-- fill: becomes the UTC time at sign-off -->
- Security impact: <none | what changes, with SR/SB ids> <!-- ask always: personal data, authentication, public endpoints, new dependency -->

## Intent

<!-- ask: what is possible or different afterwards, and why. At most 5 lines, in the requester's words. End with "Out of scope: …". -->

## Stories

<!-- ask: one story per distinct goal. Numbering continues after the highest US in the baseline. Write "none" instead of a story if the change adds no new goal (a removal, or a change to existing behaviour). -->

### US-<nnn> <title>

As a <role> I want <goal> so that <benefit>.
In scope: <…>. Out of scope: <…>.

## Rules and data

<!-- delete this section if unchanged. ask: new or changed business rules and fields. A MODIFIED row holds the complete new text. Terms in the baseline glossary are not redefined. -->

| ID | Change | Rule |
|---|---|---|
| BR-<nn> | <ADDED / MODIFIED / REMOVED> | <rule> |

## Design and quality

<!-- delete this section if unchanged. ask: new or changed architecture constraints, security requirements, non-functional requirements, extra done criteria. -->

| ID | Change | Requirement | Checked by |
|---|---|---|---|
| <AR / SR / NFR / DoD-P>-<nn> | <ADDED / MODIFIED / REMOVED> | <requirement> | <test, scan or demonstration> |

## Baseline items changed

<!-- fill: every existing ID that this CR MODIFIES or REMOVES, one row each; "none" if the CR is purely additive. The new text is in this file or in acceptance-criteria.md; the old text is the baseline's and is not copied. -->

| ID | Action | Reason |
|---|---|---|
| <ID> | <MODIFIED / REMOVED> | <why the old wording no longer holds> |

## Must not change

<!-- ask, then look up: baseline behaviour this change could plausibly disturb, as IDs or ranges (AC-001-01..07). The impact analysis may add to this list, never remove from it. -->

<IDs>

## Open questions

<!-- every row answered before sign-off. Numbering continues after the highest OQ in the baseline. -->

| ID | Question | Answer |
|---|---|---|
| OQ-<nn> | <question> | <answer> |

The answers are the requester's decisions; the agent records no decision for them.
