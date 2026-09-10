# Custody Audit Control

An enterprise-oriented Java 21 / Spring Boot platform for approval-controlled custody reference data, maker-checker workflows, and tamper-resistant audit evidence. It records successful and failed authentication, validation and application failures, endpoint activity, suspicious behavior, workflow submissions and decisions, and report generation with one traceable evidence model.

## Project identity

The product and repository are named **Custody Audit Control**, with the folder, Maven parent artifact, Spring application name and Sonar key set to `custody-audit-control`. This name is preferred over the generic `audit-trail-control` because it communicates both the custody business boundary and the system's audit/control responsibility.

Existing Java packages (`com.custody`), module artifact IDs (`cc-*`), database location and `/api/v1` routes remain stable. They are internal or persisted compatibility boundaries and do not need a breaking rename merely to change the product identity. New product-facing assets should use **Custody Audit Control** or `custody-audit-control` consistently.

## Run locally

Requirements: Java 21 and Maven 3.9+. On macOS the run script selects the installed Java 21 runtime.

```sh
./scripts/run.sh --server.port=8091
```

Open <http://127.0.0.1:8091>. The server binds to loopback by default. The script activates **dev,demo** by default, which creates synthetic records and these users on the first run:

| User | Purpose |
|---|---|
| `maker` | Submit changes; administration and CSA access |
| `checker` | Independently approve maker changes; administration and CSA access |
| `reviewer` | Bullion Clearing feature/report access |
| `other` | Separate group and `OTHER` data scope |

Default demo password: `CustodyDemo!2026`. Set `CC_DEMO_PASSWORD` **before first startup** to override it. Changing that environment variable after seeding does not reset existing passwords. Demo credentials are for local evaluation; dev without demo and the deployed profiles do not seed accounts.

Data persists in `.runtime/custody.mv.db`. Do not delete this file if you want to preserve your records. Back up a stopped database or use a supported database backup process; copying a live file is not a verified backup.

## Try the complete workflow

1. Sign in as `maker`. Open **Vault** and submit an addition or edit. It appears under **Pending approvals**; the approved record remains unchanged.
2. Sign out and sign in as `checker`. Review the before/proposed values, enter a reason and approve or reject.
3. Open **Audit trail**. Filter by feature, record, field, user, action, status, reference, screen, scope or date.
4. Choose **Generate report**, select CSV or XLSX, then open **Reports & schedules** and refresh after the worker runs. Download the Ready artifact.
5. Sign in as `other` to verify that Bullion records and artifacts are unavailable.
6. In **Users & groups**, submit a membership/group/permission change. Another administrator must approve it. Current memberships are evaluated on later access checks.
7. The seeded daily CSA schedule produces administrator evidence, including explicit no-activity reports, for currently eligible members of the CSA group.

CSV imports are available from each feature screen. The dialog displays the exact header. Imports submit up to 100 additions atomically; each row still requires independent approval. Example: `docs/vault-import.csv`.

## Structure

| Maven module | Responsibility |
|---|---|
| `cc-core` | Shared enums, error codes, secure IDs, audit-writing contract and JSON boundary utility |
| `cc-identity` | Users, groups, memberships, scoped grants and independently approved security changes |
| `cc-audit` | Append-only evidence, scoped query predicates and response projection |
| `cc-workflow` | Registered feature definitions, add/edit/status proposals, independent decisions and import orchestration |
| `cc-reporting` | Durable jobs, CSV/XLSX strategies, secure artifact storage and schedules |
| `cc-application` | Boot wiring, controllers/DTO boundary, security, tracing, jobs, migrations and web UI |

The same-origin UI uses browser JavaScript with no frontend build/runtime dependency. The Java source uses Google Java Format, enforced by Spotless. PMD runs during `mvn verify` as a local static-analysis gate, while the parent POM exposes stable Sonar project metadata for CI. No public controller returns a JPA entity. OpenAPI is generated from the actual mappings at `/v3/api-docs` after authentication.

See [the PRD](docs/audit-trail-prd.md), [design and extension guide](docs/architecture.md), [package structure](docs/package-structure.md), and [implementation scope](docs/implementation-status.md).

One canonical import-ready Postman collection covers every controller/security endpoint, complete bodies, path and query parameters, saved success/error responses, maker-checker chaining, authentication/authorisation failures and the safe 500 contract. It and its matching environment are documented in [the Postman guide](docs/postman.md).

## Configuration

Settings are in `cc-application/src/main/resources/application.properties` and can be overridden using Spring Boot properties/environment variables.

| Property | Default | Meaning |
|---|---|---|
| `cc.retention.years` | 7 | Calendar years from immutable UTC event time |
| `cc.retention.minimum-years` | 7 | Explicit configurable lower bound; startup rejects a shorter retention period |
| `cc.retention.enabled` | false | Whether the scheduled expiry process runs |
| `cc.retention.dry-run` | true | Records eligibility without deleting evidence |
| `cc.retention.batch-size` | 1000 | Maximum deletions in one scheduled batch |
| `cc.retention.cron` | `0 0 3 * * *` | Cleanup schedule in UTC |
| `cc.reports.expiry-days` | 7 | Generated artifact availability, independent of evidence retention |
| `cc.reports.max-rows` | 10000 | Maximum evidence events per report; excess fails clearly |
| `cc.jobs.poll-ms` | 5000 | Queue/schedule polling interval |
| `cc.audit.suspicious.failed-login-threshold` | 5 | Failures for one source-IP/username pair before emitting a high-risk suspicious event |
| `cc.audit.suspicious.window-seconds` | 600 | Sliding failed-login detection window |
| `cc.audit.suspicious.maximum-tracked-subjects` | 10000 | Memory bound for the local detector |

To activate deletion, explicitly set `cc.retention.enabled=true` and `cc.retention.dry-run=false`. Each execution handles a bounded batch; select frequency/batch size to match the measured backlog. Retention is startup-configured, not live-reloaded. Expiry uses `occurredAt < now.minusYears(years)` in UTC, with Java calendar leap-day adjustment.

Database environment variables: `CC_DB_URL`, `CC_DB_APP_PASSWORD`, `CC_DB_ADMIN_PASSWORD`, `CC_DB_RETENTION_PASSWORD`. The local migration creates restricted application and retention users. Password overrides used in initial provisioning must match on later starts; rotating an existing database credential requires an explicit administrative migration. Migrations connect as the administrator; ordinary JPA work uses `cc_app` with **SELECT/INSERT only on audit evidence**. Cleanup uses `cc_retention` with SELECT/DELETE on evidence. Normal users cannot access database credentials or invoke a cleanup API.

The normal profile does not include user provisioning or SSO setup. Resolve the production identity provider/provisioning process before deploying beyond local evaluation.

## Verify

```sh
./scripts/verify.sh
```

Runs compilation, positive/negative integration tests, concurrency and transaction tests, architecture checks and format verification. Tests use a separate in-memory H2 database and a subclass-based Mockito test double (no runtime agent attachment).

With the demo server running:

```sh
CC_BASE_URL=http://127.0.0.1:8091 python3 scripts/smoke.py
```

The smoke test signs in, searches evidence, creates and downloads a CSV report, and checks OpenAPI. It creates a report and related audit events in the demo database.

## Trace a failure

Every application response includes `X-Correlation-ID`; error DTOs also include `traceId`, `code`, a safe message and field errors. Search the structured application logs using that ID. Report IDs link queue execution to the initiating request. IDs use 128 bits from `SecureRandom`. They are support references, never authorisation tokens.

Logs go to stdout in ECS JSON format. Configure your operational collector, access controls and log retention for deployment. Operational logs do not replace persistent audit evidence.


## Environment profiles and MySQL roadmap

**H2 is only for local development and tests. MySQL is the intended production database.**

| Profile | Properties file | Database / status |
|---|---|---|
| dev (default) | application-dev.properties | H2; implemented and integration-tested |
| sit | application-sit.properties | MySQL connection template; schema provisioning/testing pending |
| uat | application-uat.properties | MySQL connection template; schema provisioning/testing pending |
| prod | application-prod.properties | MySQL connection template; schema provisioning/testing pending |

Shared behaviour lives in `application.properties`. SIT/UAT/prod import `application-mysql.properties` to share driver, pool, secret and secure-cookie settings. Choose exactly one environment, in lowercase. `demo` implies dev for compatibility; combining environments or demo with a deployed profile fails before datasource creation.

```sh
# Local development with synthetic users and data
./scripts/run.sh --server.port=8091
# Local development without seeding
SPRING_PROFILES_ACTIVE=dev ./scripts/run.sh --server.port=8091
# Future deployed environment, once the MySQL schema and accounts are provisioned
java -jar cc-application/target/cc-application-1.0.0-SNAPSHOT.jar --spring.profiles.active=sit
```

For SIT/UAT/prod supply `CC_DB_URL` (a jdbc:mysql URL), `CC_DB_APP_USERNAME`, `CC_DB_APP_PASSWORD`, `CC_DB_RETENTION_USERNAME` and `CC_DB_RETENTION_PASSWORD` through your deployment secret/configuration system. No development credentials are inherited. Use a TLS-verified database connection according to deployment requirements; these web profiles expect HTTPS and set secure session cookies. `CC_DB_POOL_SIZE`, `CC_DB_POOL_MIN_IDLE`, `CC_BIND_ADDRESS`, `CC_RETENTION_ENABLED` and `CC_RETENTION_DRY_RUN` are optional overrides.

The MySQL JDBC driver is included, but the MySQL schema migration is **not delivered or tested yet**. Application Flyway is deliberately disabled for these profiles; Hibernate validates an externally provisioned schema and will fail on missing tables. Before rollout, deliver vendor-specific migrations and least-privilege accounts/grants, then validate MySQL transactions, locking, UTC timestamps, large fields, retention and evidence immutability. H2 migration files stay under `db/migration/h2` and must not be executed against MySQL. Do not enable Hibernate schema auto-update as a workaround.

## Java 21 / IDE compilation

Open/import **custody-audit-control/pom.xml**, the parent reactor, so all six modules resolve together. Set the project SDK, language level and Maven runner/importer JDK to **21**, then reload Maven projects and rebuild. The build rejects other Java major versions with a clear message.

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean verify
```

`SecurityChangeResponse` is a Java record in `com.custody.identity.dto.response`; its eight constructor arguments match `IdentityResponseMapper`. It compiled in the clean reactor build. If the IDE still flags this type after reload, capture the exact diagnostic and line; a successful Maven build does not prove the IDE's own configuration is correct.


## Pluggable audit capture and banner

A startup banner lives at `cc-application/src/main/resources/banner.txt`. It identifies Custody Audit Control and shows the Spring Boot version without credentials. Banner mode is explicitly set to log so it appears as one structured startup log event. Standard `spring.main.banner-mode=off` disables it when desired.

For new business events, inject `AuditCapture`, provide a typed event and register one `AuditEventAdapter<T>` bean in the owning feature module. The central registry discovers adapters at startup and delegates to the existing transactional AuditWriter. All six setup features now use one shared adapter; imports, administration and reporting retain their compatible direct writer integration. See [the integration guide](docs/audit-integration.md) for the directory tree, real code example, transaction rules and extension steps.

## Repeated maker/checker changes

Every submission and every decision is an append-only audit event. A change-request ID is the stable workflow reference joining one maker submission to its checker decision; the feature, scope and business key join successive change requests into the record's complete history. Only one request for a record may be pending at a time, but after approval or rejection the maker can submit another change. Prior cycles are never updated or replaced.

Audit snapshots have three distinct meanings:

| Snapshot | Meaning |
|---|---|
| `before` | Effective state when the maker submitted the request |
| `proposed` | Validated state requested by the maker |
| `after` | State that actually became effective; unchanged from `before` for pending/rejected requests |

This prevents a rejected Loco Singapore, Holiday Calendar, Vault, or future feature proposal from appearing as an applied value. Search, UI detail, CSV and XLSX use the same model. Current field-level access restrictions are applied to all three snapshots. A new feature using the common setup workflow receives this behavior without editing the audit module. A richer domain can use the same contract by setting `.proposed(...)` in its `AuditDraft`; activity-only integrations may omit it and receive an empty immutable map.

## Login, error, endpoint and suspicious-activity audit

All handled API requests now create endpoint evidence. Successful and failed authentication, logout, unauthenticated access, access/CSRF denial, DTO validation, domain conflicts, infrastructure errors and unexpected exceptions are classified explicitly. HTTP/security evidence uses an independent `REQUIRES_NEW` transaction so a rejected or rolled-back request does not erase its evidence. Committed business mutations retain the stricter existing rule: their domain evidence is written in the same transaction and audit failure rolls back the mutation.

The audit report includes schema/event type, normalised endpoint template, method, HTTP status, outcome, safe error code, actor or attempted login subject, direct peer IP, bounded user agent, risk level, duration and correlation ID. Passwords, cookies, authorisation headers, CSRF tokens and unrestricted request/response bodies are never recorded. Five failed logins for the same direct-IP/username pair within ten minutes produce `SUSPICIOUS_ACTIVITY` at `HIGH` risk by default. This bounded local detector is useful for one instance; multi-node correlation, proxy-aware trusted client IP and advanced behavior analytics belong in the enterprise SIEM/rate-limit platform.

Backend route templates are centralised in `ApiRoutes`, including parameterised child paths. Set `CC_CONTEXT_PATH` to relocate the whole application without editing controllers; the browser derives its API location from the document base. Changing the versioned `/api/v1` contract remains an intentional source/API change in one Java catalogue and one browser catalogue.

## Typed exceptions and global error handling

Each module now owns a customisable `ErrorCatalog`: every `ErrorDefinition` independently declares its stable public code, HTTP status, safe message key, handling category and retry policy. A small sealed exception hierarchy handles validation, authentication, authorisation, lookup, conflict, business, infrastructure and internal categories, avoiding one empty class per code. Spring discovers catalogues and fails startup on duplicate codes or missing messages. One global handler consistently produces safe `application/problem+json`, attaches the server-generated trace ID and exposes the same feature code to endpoint audit capture. Generic audit outcome remains the separate `SUCCESS`/`FAILURE` enum.

See [the exception architecture](docs/exception-architecture.md) for the flow diagram, custom code examples, catalogue and exception-category tables, trace generation, audit rules and feature extension checklist. The complete generated API collection is described in [the Postman guide](docs/postman.md) and stored under `docs/`.
