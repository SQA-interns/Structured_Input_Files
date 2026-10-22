# Tech stack: CR-<nnn>

> Written in: change interview · Source: requester's answers · Agent: writes until sign-off, then read-only

Only the entries this change adds or changes, with the fields and rules of the baseline `tech-stack.md` (exact versions, compatible licence, scanned like any other). An entry whose id is already in the baseline replaces it. Signing the request is the human's approval of these entries.

## Dependencies

<!-- ask: which libraries does the change need that the baseline does not have, and why? Look up, do not ask: what the baseline already provides. Exact versions only; ecosystem, scope, component, source, purpose and licence as in the baseline entries. -->

```yaml
dependencies:
  - { id: <coordinates>, ecosystem: <maven / npm>, version: <exact version>, scope: <build / runtime / test>, component: <name>, source: <registry>, purpose: <why>, license: <spdx id> }
```

## Platforms and tooling

<!-- delete this section if unchanged. Same schema as the baseline `platforms` and `tooling` entries, including `version_check`. -->
