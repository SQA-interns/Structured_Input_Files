# Cycle plan

> Written in: phase 4, once, before the first cycle · Source: `project/01_requirements/acceptance-criteria.md`, `project/01_requirements/scope.md`, `docs/02_specification.md`, `docs/03_test-strategy.md` · Procedure: `general/skills/tdd-cycle` · Agent: writes

<!-- One row per cycle in the order it is built (priorities in scope.md). A cycle is one AC or a range of ACs of one story that share one code path. Not updated during the build: progress goes to 00_progress.md, the git history shows the cycles as built. -->

| Cycle | AC or range | Story | Component | Test classes to add (red) | Frozen tests it turns green |
|---|---|---|---|---|---|
| C01 | AC-003-01..03 | US-003 | backend | `ConferenceConfigurationTest` (domain rules: duplicate id, category, name, default availability), `ConferenceConfigurationLoaderTest` (file → model, startup error message), `FormConfigControllerTest` (MockMvc slice), `ArchitectureTest` (AR-02, AR-03) | `FormConfigAcceptanceTest`, `ChangedConfigurationAcceptanceTest`, `InvalidConfigurationAcceptanceTest` |
| C02 | AC-001-01..07 | US-001 | backend | split by layer: `RegistrationValidatorTest` (external fields, trim, email, options, consents), `RegisterParticipantTest` (use case with fake ports, captcha), `JpaRegistrationStoreIT` + Flyway migration (Testcontainers), `RegistrationControllerTest` (400 field errors, 201, unknown properties) | `ExternalRegistrationAcceptanceTest` |
| C03 | AC-005-01..04 | US-005 | backend | `JsonCopyWriterTest` (file content, UTF-8, failure), `RegisterParticipantTransactionIT` (copy failure rolls back, AR-05) | `StorageAcceptanceTest` |
| C04 | AC-002-01..06 | US-002 | backend | `RegistrationValidatorTest` student cases (student fields, availability by type) | `StudentRegistrationAcceptanceTest` |
| C05 | AC-006-01..03 | US-006 | backend | `ParticipantMailComposerTest` (plain text, fixed headers), `MailNotifierTest` (after commit, failure logged with id only) | `ParticipantEmailAcceptanceTest`, `MailServerUnavailableAcceptanceTest` |
| C06 | AC-007-01..02 | US-007 | backend | `OrganizerMailComposerTest` (one message per address, JSON attachment equals copy) | `OrganizerNotificationAcceptanceTest` |
| C07 | AC-008-01..04 | US-008 | backend | `ExcelExportWriterTest` (columns, string cells, order), `ExportSecurityTest` (401, BCrypt, HTTPS-only 403, loopback) | `ExportAcceptanceTest` |
| C08 | SR-01, SR-02 (DoD-P05) | US-001 | backend | `RecaptchaVerifierTest` (production path against a mock verify endpoint, rejected token, test mode), `ProductionSafetyTest` (start refused with test mode on or empty keys) | none (no AC) |
| C09 | SR-03, SB-10, NFR-04 | US-001 | backend | `RateLimitFilterTest`, `RequestSizeLimitTest`, `SecurityHeadersTest`, `HealthEndpointTest` | none (no AC) |
| C10 | AC-001-08 | US-001 | frontend | `api.test.ts` (form-config client), `RegistrationForm.test.tsx` (type choice, external fields, groups, consents) | `externalForm.acceptance.test.tsx` |
| C11 | AC-002-07 | US-002 | frontend | `RegistrationForm.test.tsx` student cases, `options.test.ts` (filter and group by type) | `studentForm.acceptance.test.tsx` |
| C12 | AC-004-01..04 | US-004 | frontend | `validation.test.ts` (required, email), `api.test.ts` (submit, 201/400/other), `App.test.tsx` (confirmation, field errors, general error) | `confirmation.acceptance.test.tsx`, then E2E-01..05 against the stack |

Order follows `scope.md`: the options configuration first (every registration needs it), storage (C02, C03) before notifications (C05, C06), participant flow before the organizer export (C07), then the controls without their own criterion, then the frontend. The end-to-end tests turn green after C12, when both components run in the compose stack.
