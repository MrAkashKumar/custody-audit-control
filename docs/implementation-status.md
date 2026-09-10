# Implementation scope and decisions

This is an executable local application implementing the agreed core workflows. The PRD also contains open business decisions and future production requirements; they are not silently treated as verified.

## Implemented

- Java 21, Spring Boot, six Maven modules, JPA, H2 file persistence and Flyway migrations.
- Six registered setup screens with add/edit/view and approval-controlled activation/deactivation.
- Distinct maker/checker, pending proposals, rejection, immutable evidence and concurrency constraints.
- Repeated maker/checker cycles with separate before/proposed/effective-after evidence in search, UI and exports.
- Dynamic groups/memberships/grants, approved administration and audited changes.
- Scoped audit filters, pagination, detail, controlled before/after projection.
- Manual CSV/XLSX jobs, secure downloads, current row/field authorisation and artifact expiry.
- User-owned daily/interval schedules with optional group audiences, occurrence uniqueness and null reports.
- CSV import of additions with batch evidence and per-row pending proposals.
- Properties-configured retention with an explicit floor, dry-run and restricted deletion identity.
- DTO boundaries, CSRF-protected session login, safe errors, SecureRandom correlation IDs and structured logs.
- Audited login success/failure, logout, API outcomes, validation/access/system errors, endpoint templates and bounded repeated-login anomaly detection.
- Automated positive/negative integration, rollback, concurrency, retention, field-restriction and architecture tests.
- Generated authenticated OpenAPI at `/v3/api-docs`.

## Explicit initial choices

- The six feature fields are representative definitions, because their full domain schemas were not supplied. They are editable in FeatureConfiguration. Fee calculation, physical-inventory accounting and location/vault dependency rules require business specification.
- One pending proposal per feature/scope/business key. Approved additions start Active. Reasons are required. No delete operation is exposed for setup records.
- One global audit retention policy; minimum seven years unless explicitly reconfigured. Separate generated-file expiry.
- Reports are bounded to 10,000 events by default. Querying large archives requires measured tuning. Setup list currently returns up to 500 records per feature; broader listing requires a paginated feature UI/API extension.
- Scheduled reports run at Singapore midnight and cover complete previous calendar days. Schedules advance one occurrence at a time, so outages can backfill without incorrectly labelling generation failure as no activity.
- Field restrictions are optional comma-separated `hiddenFields` on a permission grant; values are omitted, not partially masked. Fine-grained redaction formats are not specified.
- CSV imports are all-or-nothing additions, up to 100 rows. Edit imports and partial-success imports require a specified transaction policy.

## Not represented as completed production requirements

- Full business modules for account transactions, inventory movements, transfers, allocations or accounting entries. The transactional audit contract is ready for those modules; no financial processing behaviour is fabricated.
- OS/process logging, enterprise identity/SSO, user provisioning and central operational log collection.
- Legal holds, archive-before-delete, backup retention automation and proven disaster recovery.
- Independently service-owned group schedules, external delivery and email. Secure downloads are implemented as requested.
- Seven-year capacity/load certification, recovery objectives and multi-node production deployment.
- A formal external penetration test or browser automation/accessibility certification. HTTP flows, frontend syntax and backend tests are verified.

Use these items to close the remaining PRD decisions before a production rollout. The original PRD remains a requirements document, not a claim that every proposed future capability is already implemented.


## PRD v1.8 structure revision

Implemented responsibility packages, service interfaces with implementation classes, private entity fields, standalone request/response DTOs, a typed administration-directory response, response mappers, explicit imports and descriptive constructor dependencies. Report user, scheduler and worker contracts are separated. Architecture rules now verify these boundaries in addition to module acyclicity. This is a structural refactor; the existing functional limitations above still apply.


## Environment configuration update

Dev uses H2; SIT/UAT/prod have shared MySQL connection configuration with external credentials and no H2 fallback. Profile validation and JDK 21 enforcement are implemented. MySQL is the planned production database, but MySQL DDL migrations, least-privilege account provisioning and database integration testing remain pending. The deployed profiles require a separately provisioned schema and keep application Flyway disabled until the MySQL migration delivery.
