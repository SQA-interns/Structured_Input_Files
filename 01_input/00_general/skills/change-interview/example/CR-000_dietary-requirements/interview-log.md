# Interview log: CR-000

> Written in: change interview · Source: the conversation · Read in: post-run report only, never during a change run · Agent: appends only

One row per question, appended after each round. Source: `human` (decided by the requester), `default` (the recommended answer was accepted), `looked up` (answered from the baseline or the code, not asked).

| # | Question | Recommended | Answer | Source | Affects |
|---|---|---|---|---|---|
| 0 | Is the baseline complete and unchanged? | yes | Phase 7 complete, gate met, no open Critical or High, manifests match | looked up | Baseline |
| 1 | Is this an add, a change or a remove? | change: the feature extends the existing forms | change | default | Type |
| 2 | Which registration types get the field? | both | both | default | US-009, BR-09 |
| 3 | Is the field required? | optional | optional | default | BR-09 |
| 4 | Which fields do the forms have today, and which criteria say so? | n/a | BR-01 and AC-001-08, AC-002-07 list them | looked up | BR-01, AC-001-08, AC-002-07 |
| 5 | Which existing criteria and scenarios stop being true or incomplete? | n/a | AC-001-08, AC-002-07; E2E-04 must also cover the field | looked up | AC-001-08, AC-002-07, E2E-04 |
| 6 | What is the maximum length? | 500 characters | 500 characters | default | BR-09, OQ-07, AC-009-03 |
| 7 | Is the text shown on the confirmation page? | no: AC-004-01 stays unchanged | no | default | OQ-08 |
| 8 | Does the change touch personal data, authentication, endpoints or dependencies? | personal data only | personal data only | default | Security impact |
| 9 | Is dietary information special-category data that needs its own consent? | yes: explicit consent under GDPR Art. 9 | no: ordinary personal data, demonstration choice | human | OQ-09 |
| 10 | Which baseline behaviour must not change? | validation, JSON copy, attachment, export access, unaffected scenarios | accepted | default | Must not change |
