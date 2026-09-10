# Verification

Verified on 9 September 2026 using Java 21.0.10.

- `mvn -o -q spotless:apply verify`: passed.
- 25 integration tests: passed, zero failures/errors/skips.
- 9 architecture tests: passed, zero failures/errors/skips.
- 4 audit draft unit tests: passed, zero failures/errors/skips.
- JavaScript syntax check: passed.
- Live HTTP smoke after the final restart: passed (session login + CSRF, six features, audit query, queued CSV generation, secure download, generated OpenAPI).

Coverage includes self-approval denial, stale edits, duplicate/concurrent approval, audit-write rollback, database-enforced evidence immutability, cross-group isolation, field restrictions and revoked artifact access, real CSV/XLSX parsing, null reports, scheduled reporting, retention dry-run and actual cutoff deletion, import transaction atomicity, formula-safe CSV output, DTO validation, trace correlation and module boundaries.

Performance/load certification and automated browser/accessibility testing have not been performed. See implementation-status.md for the remaining domain and deployment decisions.


## PRD v1.8 package refactor

Verified after the package and service-contract refactor: 22 integration tests and 8 architecture tests, all passing. `mvn -o -q spotless:apply verify` and JavaScript syntax verification passed. The application was restarted against the existing H2 database; the live HTTP smoke test passed for authentication/CSRF, six feature definitions, audit search, background CSV generation, secure download and generated OpenAPI.

Additional architecture checks cover private entity fields, repository placement, public service interfaces, implementation hiding, cross-module dependencies, standalone DTO records and responsibility-based package placement. No database migration was introduced by this refactor.


## PRD v1.10 reusable audit capture

`mvn -o -q spotless:apply verify` passed with 38 tests: 25 integration, 9 architecture and 4 audit draft tests. New checks cover immutable snapshot ownership, missing/null evidence, empty activity snapshots, mandatory writer transactions, explicit rollback removing evidence, propagation of a context trace and unique secure fallback traces. The existing injected sink-failure test now targets the separate AuditWriter adapter and continues to prove business rollback. Architecture checks keep query/write contracts separate and business modules independent of audit implementation details.

No schema or HTTP endpoint changes were introduced. Generic interceptor/AOP capture, outbox delivery, and production load certification are not part of this refactor.

The rebuilt application was restarted on port 8091 against the existing H2 database. The live HTTP smoke passed for login/CSRF, six features, audit search, queued CSV generation, secure download and OpenAPI.


## PRD v1.11 naming and readability

Replaced 298 shorthand declarations and their resolved Java references across production and tests. Clarified audit predicate/response helpers, report access/activity helpers and exception-handler names. Public endpoint paths, DTO field names and database mappings remain unchanged.

Java 21 `mvn -o -q spotless:apply verify` passed: 25 integration, 9 architecture, 4 audit draft and 1 source naming test (39 total; zero failures/errors/skips). The source test rejects prohibited shorthand in production and test declarations, including lambda parameters. Semantic readability remains a review responsibility.

After restarting the rebuilt application on port 8091, the HTTP smoke passed: login/CSRF, six features, audit search, CSV generation, secure download and OpenAPI.


## PRD v1.12 environment profiles

The clean Java 21 baseline compiled SecurityChangeResponse and its mapper/service/controller usages successfully; the user's IDE-specific diagnostic was not reproduced. The final `mvn -o -q spotless:apply clean verify` passed all 46 tests: 25 integration, 9 architecture, 4 audit draft, 1 naming and 7 environment-profile cases. Local Maven artifacts were refreshed with `mvn -o -q -DskipTests install` after verification.

Profile tests cover default dev, demo compatibility, SIT/UAT/prod configuration loading, missing-secret behaviour, secure cookies, schema validation, and rejection of mixed profiles or deployed H2. Existing retention deletion tests pass with the portable bounded-selection implementation. MySQL connectivity, schema provisioning and MySQL integration tests were not performed; MySQL profiles remain preparation for the planned migration.

The app restarted with dev,demo against the existing file database, preserving Flyway checksums after moving H2 scripts. The live HTTP smoke passed login/CSRF, six features, audit search, CSV generation, secure download and OpenAPI.


## PRD v1.13 pluggable audit integration

Java 21 `mvn -o -q spotless:apply verify` passed 51 tests: 26 integration, 9 architecture, 7 profile, 4 draft, 4 adapter registry and 1 naming test. The setup workflow now exercises adapter-based capture in the existing business regressions, including injected audit-write failure rollback. Additional tests cover registry discovery/dispatch, duplicate registrations, unknown/null events, missing drafts, adapter exceptions and mandatory capture transactions.

After setting explicit banner log mode, packaging passed with tests skipped (only banner configuration/documentation changed). The startup log shows the custom CUSTODY AUDIT CONTROL banner and resolved Spring Boot version. The app restarted on port 8091, and the live smoke passed authentication/CSRF, six features, audit search, CSV generation, secure download and OpenAPI. This is an in-application extension contract, not a hot-loadable plugin system or standalone Boot starter.


## PRD v1.16 typed exception boundary

Java 21 `./scripts/verify.sh` passed the complete seven-module reactor with 57 tests, zero failures/errors/skips, JavaScript syntax validation and clean Spotless formatting. The scalable catalogue revision adds startup-validation tests for duplicate codes and missing messages. Coverage includes application integration, architecture rules, environment profiles, audit capture/drafts and source naming.

New regressions prove that module-owned catalogues have unique custom public codes and message keys, all eight sealed exception categories are covered, and missing endpoints, unsupported methods and unsupported media types use the central safe problem response. Existing tests continue to cover validation, authentication, trace correlation, endpoint/security audit, maker/checker behavior, rollback and persistence constraints. No database schema migration was required for this exception refactor.


## SRP exception boundary and static analysis

Java 21 `./scripts/verify.sh` passed the complete seven-module reactor with 61 tests, zero failures/errors/skips, JavaScript syntax validation, clean Spotless formatting and zero PMD violations. PMD 7.3.0 is now bound to Maven `verify`, so local and CI builds fail when a new finding is introduced.

Regression tests prove that every concrete exception rejects an `ErrorDefinition` from the wrong category while preserving the exact feature-owned public code. Handler support tests cover nested SQL-state/vendor-code classification and deterministic, null-safe field validation mapping. The global handler delegates response construction, binding-error mapping and database classification to focused collaborators.

No SonarQube/SonarCloud server, organisation or authentication token is configured in the workspace, so a remote Sonar quality-gate result was not fabricated. The parent POM now supplies the stable project key, project name and source encoding required by CI; deployment credentials remain external.
