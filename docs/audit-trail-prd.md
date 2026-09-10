# Senior Java Architect and Engineer

# Audit Trail and Evidence Reporting — Product Requirements Document

Version: 1.15  
Date: 10 September 2026  
Status: Living requirements; local implementation exists, with verification and limitations tracked separately  
Business context: Custody / Bullion Clearing

## 1. Purpose and intended outcome

Provide authorised users with reliable evidence of who performed an activity, what changed, when it happened, why it happened, and how it was approved. Evidence must be searchable and exportable under the access matrix, protected from normal-user alteration, and retained until its configured expiry.

The product includes three views over a shared audit capability:

1. Static-data change reporting.
2. Business-activity search and export.
3. Daily administrator-activity reporting for CSA.

The implementation must support new business modules and event types without rewriting the shared capture, search, export or retention capabilities.

## 2. Requirement status and technology constraints

**Confirmed:** Java 21, Spring Boot, a multi-module architecture, JPA and H2; three audit reporting capabilities; before/after evidence; maker/checker information; access-controlled search and export; properties-based retention and expiry deletion; no alteration by normal users.

**Confirmed setup workflow:** Holiday Calendar, Physical Gold, Loco Singapore, Vault, Fee Schedule and Managed Reference Data each support Add, Edit and View. Additions and edits require approval by a different authorised user before taking effect. Tables expose Active/Inactive status controls. Approval for status changes is proposed below, pending confirmation.

**Retention clarification:** The initial request required seven years of searchability. The subsequent request makes the number of years configurable. Seven years is the proposed default. Whether it is also a mandatory minimum is an open business decision; arbitrary shortening must not be assumed.

**Proposed:** A modular monolith with transactional capture, a common audit event model, and separated reporting and retention responsibilities. Operational defaults and performance targets below are proposals, not approved requirements or measured results.

**Unresolved:** H2's production role, traffic and storage volumes, access-matrix details, generated-file expiry and the final retention rules.

## 3. Users and access

| Persona | Required capability |
|---|---|
| Bullion Clearing reviewer | Search permitted static-data changes and inspect evidence |
| Authorised business user | Search permitted business activity and export authorised fields |
| Maker | Initiate business changes; identity recorded by the backend |
| Checker | Approve or reject changes through the business approval workflow |
| CSA reviewer | Access daily administrator reports, including no-activity reports |
| Operations role | Monitor capture, report generation, exports and retention jobs |
| Retention service identity | Execute expiry deletion under dedicated permissions |

The exact roles and scope rules must be provided by the business. Audit viewing does not automatically grant export, approval or retention-policy permissions. Users may hold multiple roles only as allowed by the access matrix.

## 4. Scope

### Included

- Six named static-data setup types with approval-controlled add/edit and Active/Inactive controls, as detailed in Section 16. Deletion is not requested for these screens; generic audit capture must support deletion only where another business workflow permits it.
- Account, inventory, transfer, allocation, fee, limit, accounting and administration activities.
- User ID, role and group maintenance.
- Business changes initiated through screens, APIs, imports and scheduled processing.
- Import and export lifecycle evidence, including completion and failure outcomes.
- Relevant accounting-entry identifiers and outcomes.
- Approval history, filtered search, event detail and controlled exports.
- Daily CSA reports and explicit no-activity reports.
- Configurable retention, expiry cleanup, operational monitoring and validation.

### Scope boundaries

- Form edits that have not been submitted are not persisted business changes.
- Page access, failed authentication, failed business attempts and operating-system events are distinct activity categories; their coverage must be agreed rather than inferred from entity changes.
- The attached policy requires operating-system, application-process and user activity logging and monitoring. OS and infrastructure coverage needs an agreed integration and operational owner; the application reports alone do not satisfy that entire policy.
- Business approval rules remain owned by business workflows. The audit product evidences them.
- Event sourcing, microservices and a general-purpose security monitoring platform are not required for the first release.

## 5. Product principles

1. **Backend authority:** Actor identity, trusted timestamps and actual persisted values come from the backend.
2. **Complete committed evidence:** A required business change and its audit evidence commit together or both fail.
3. **Preserved history:** Approval decisions and corrections append events rather than rewrite previous evidence.
4. **Consistent authorisation:** Search, detail, counts and exports use the same access rules.
5. **Explicit business meaning:** Business modules identify the action and reference; shared components handle common audit mechanics.
6. **Controlled expiry:** Normal users cannot delete evidence; a dedicated process deletes eligible evidence under the configured policy.
7. **Measured scale:** Capacity claims require representative volume and load tests.

## 6. User journeys and interface

### 6.1 Search and inspect

The reviewer selects Static-data changes, Business activity or Administrator activity, applies filters, and receives only authorised results. Opening an event shows its business reference, source, actor, reason, field-level comparison and related approval history.

A pending change clearly distinguishes **current value** from **proposed value**. A completed change shows **before value** and **after value**. The interface must not imply that a pending or rejected proposal was applied.

The audit interface is read-only. Approval actions, if linked, lead to the separately authorised business workflow.

### 6.2 Export

The user exports all matching authorised results, not merely the visible page. The export preview states the filters, timezone, record count or pending estimate, format and controlled field selection. Large exports show requested, running, completed or failed status.

Exports are bounded by a recorded query cutoff so that newly arriving events do not silently change an export already in progress. The system applies the approved retention/export coordination rule if source evidence expires during processing.

### 6.3 Daily administrator report

The CSA reviewer selects a reporting day and opens the generated report. A completed day without qualifying events produces an explicit no-activity report. A failed generation or missing source data must never be displayed as no activity.

### 6.4 Required UI states

- Initial search, loading, populated results and no matching results.
- Invalid date range and unauthorised detail access.
- Pending, approved, rejected and not-applicable approval states.
- Export requested, in progress, completed and failed.
- Daily report generated, no activity, pending and failed.
- Clear timezone labels and keyboard-accessible controls.

The existing wireframe is a discussion aid. Sample names, dates and format choices are not production requirements.

## 7. Functional requirements

| ID | Requirement |
|---|---|
| CAP-01 | Capture committed create, update and delete events for every configured auditable operation. |
| CAP-02 | Identify the actor from authenticated backend context; support explicit service identities for automated jobs. |
| CAP-03 | Capture relevant before/after values using an allowlisted, versioned audit snapshot. Do not record secrets or unrestricted request bodies. |
| CAP-04 | Record stable event, entity, business-reference and correlation identifiers, along with source and event timestamps. |
| CAP-05 | Capture a reason when required by the business operation. Do not manufacture missing reasons. |
| CAP-06 | Preserve evidence for deleted business records without depending on those records continuing to exist. |
| CAP-07 | Support screen, API, import and scheduled-job entry points through equivalent capture rules. |
| CAP-08 | Exclude formatting-only differences using documented comparison rules; define null, absent, decimal, date and collection behaviour. |
| CAP-09 | Repeated delivery or request retries must not create duplicate evidence for the same logical event. Distinct legitimate operations remain distinct. |
| CAP-10 | Required audit persistence failure rolls back the associated business transaction. A rolled-back operation must not leave a successful-change event. |
| CAP-11 | Record configured failed attempts as separate outcome events through a path that survives the failed business transaction. |
| CAP-12 | Reject or explicitly instrument bypass paths, including bulk operations and direct database maintenance, before declaring capture complete. |
| APR-01 | Link submission, approval, rejection and application events using a stable change-request reference. |
| APR-02 | Preserve maker, checker, decision time, effective time and status-at-event separately. A report may derive the latest status without overwriting source evidence. |
| APR-03 | Detect stale versions during update and approval; the approval workflow determines whether resubmission is required. |
| APR-04 | Preserve every maker submission and checker decision as a separate immutable event. Link the two events in a decision cycle by change-request ID and link successive cycles by stable feature, scope and business key. Store before, proposed and actually effective after values separately so pending or rejected data is never represented as applied. |
| SRCH-01 | Static-data filters include setup type, record, field, action user and date range. |
| SRCH-02 | Business filters include event type, user, reference, account, screen and date range. |
| SRCH-03 | Provide deterministic pagination and sorting with an event-ID tie-breaker for equal timestamps. |
| SRCH-04 | Apply access restrictions before pagination, totals and aggregation; protect direct event lookup as well. |
| SRCH-05 | Interpret an inclusive UI end date as the next day's exclusive boundary in the selected timezone. |
| EXP-01 | Export the same authorised field set and selection criteria represented by the report. |
| EXP-02 | Apply authorisation when generating and downloading exports, including permission changes since request time. |
| EXP-03 | Record export request, generation outcome, requester, criteria, cutoff and output reference. Record download only when observed. |
| EXP-04 | Escape exported values appropriately, including spreadsheet-formula injection protection for spreadsheet-compatible formats. |
| IMP-01 | Link import-level evidence and individual successful record changes to a batch reference; distinguish full success, partial success and failure. |
| ACC-01 | Include accounting-entry identifiers, action, outcome and relevant business references; define the accounting event catalogue before rollout. |
| ADM-01 | Report user-ID and role/group maintenance with before/after state, administrator identity and maintenance timestamp. |
| ADM-02 | Generate daily reports for CSA group R2WD_GTO_ISTOA_CSA and make them available for authorised secure download in Reports. |
| ADM-03 | Generate an explicit null/no-activity report for a complete reporting day without qualifying events. |
| ADM-04 | Support controlled retries and backfill. Reissued reports must be distinguishable from original versions. |

## 8. Evidence model

This is a conceptual information model, not a final database schema.

| Information group | Required meaning |
|---|---|
| Identity | Unique event ID, event schema version, event type and action |
| Subject | Setup/entity type, stable record identifier, business reference and account where applicable |
| Actor | Stable user/service ID and a permitted display-name snapshot |
| Time | Backend event time in UTC; separate decision/effective/recorded times where meaningful |
| Context | Screen or source channel, reason, batch/job reference and correlation ID |
| Outcome | Success/failure classification and approval status at the event |
| Changes | Stable field key, value type, before value, maker-proposed value and actually effective after value; explicit absence for create/delete |
| Relationships | Change-request ID and related-event references |
| Access scope | Durable organisational/account scope sufficient to authorise historical evidence |

One event can contain multiple field changes. Non-change activities such as exports can have no field differences. Human-readable labels must not replace stable identifiers.

Do not use mutable business-record references as the only source of historical names, scope or values. Historical schema versions must remain renderable after fields are renamed or removed.

## 9. Retention and controlled deletion

### Configuration contract

| Setting | Proposed behaviour |
|---|---|
| Retention years | Positive integer; initial default 7 calendar years |
| Minimum permitted years | Enforce only after the business confirms whether a mandatory floor exists |
| Cleanup enabled | Explicit deployment configuration; production release must select a value |
| Cleanup schedule | Daily, outside the agreed peak period |
| Batch size | Bounded deletion batches; initial proposed value 1,000 events |
| Dry-run | Available for validation before activating deletion |

One global period is proposed for release 1. Category-specific policies are a future extension unless the business requests them now. Ordinary properties changes take effect after application restart; live configuration reload is not a release-1 requirement.

### Rules

- RET-01: Calculate expiry from the immutable backend event timestamp using UTC and calendar-year arithmetic. Document leap-day behaviour.
- RET-02: Preserve each event for at least its configured period. Related records must not be cascade-deleted before their own expiry.
- RET-03: Delete an eligible event and its owned field-change details atomically. Related workflow evidence follows its own eligibility or an explicitly approved grouping rule.
- RET-04: Shortening retention can make existing records immediately eligible on the next run; deployment review and dry-run results must make this impact visible.
- RET-05: Reject invalid configuration. Configuration ambiguity must prevent destructive cleanup rather than select a shorter fallback.
- RET-06: Use dedicated privileges; ordinary users and normal capture paths cannot modify or delete historical evidence.
- RET-07: Jobs must be restartable and safe under multiple application instances. A failed batch must not invalidate completed batches or leave partial evidence.
- RET-08: Record policy/version, cutoff, job identity, start/end time, deletion count and failures in a protected operational record with its own agreed lifecycle.
- RET-09: Define retention for exports, generated reports, backups and any archive independently; removal from the live database does not imply removal from all copies.
- RET-10: The original seven-year requirement applies explicitly to static data. Applying the configured policy to all categories is proposed and must be confirmed.

Whether legal/investigation holds are required is open. If adopted, active holds override expiry deletion. Archive-before-delete is also optional, not assumed.

## 10. Design boundaries for extensibility

The following boundaries guide the subsequent technical design; they do not prescribe individual classes or framework annotations.

| Capability | Owns | Must not own |
|---|---|---|
| Business module | Business rules, event meaning, snapshot mapping and business references | Audit storage internals or export formatting |
| Audit contracts | Small stable event and recording contracts | Business entity dependencies |
| Audit capture | Context validation, comparison and evidence persistence | Business approval decisions |
| Audit query/reporting | Permission-aware search, detail and report projections | Historical evidence mutation |
| Audit export | Controlled export lifecycle and format adapters | Independent access-rule definitions |
| Audit retention | Eligibility policy and deletion execution | Normal user workflows |
| Application/operations | Wiring, authentication adapters, scheduling and monitoring | Duplicated business logic |

**Initial architecture proposal:** One Spring Boot application, business and audit evidence stored under the same database transaction. Business modules depend on small audit contracts. Reusable components use composition rather than a large inherited base service.

**Patterns with a concrete purpose:** Strategy for snapshot/comparison variations; dependency inversion for recording and persistence boundaries; adapters for storage and export formats. Separate read and write contracts. Avoid introducing patterns without a real extension need.

**Future scale:** If audit storage becomes a separate service, require a durable handoff such as a transactional outbox with retry and deduplication. An in-memory asynchronous callback alone must not become the only evidence path.

**Extension acceptance:** Adding a new setup type should require its event mapping, allowlisted snapshot and policy registration, plus module-specific validation. It must not require editing unrelated business modules or rewriting the generic report, export and retention engines. New semantics may legitimately require contract evolution and versioning.

## 11. Quality and scalability requirements

| Area | Required quality / proposed acceptance target |
|---|---|
| Correctness | No missing or duplicate successful events in rollback, retry and concurrency test scenarios |
| Integrity | No normal-user path can update or delete evidence, including direct identifier requests |
| Confidentiality | Zero unauthorised rows, counts or unmasked protected fields in acceptance tests |
| Search | Proposed p95 <= 2 seconds for a paginated filtered search on an agreed production-like dataset |
| Write overhead | Measure additional commit latency under agreed concurrency; establish a budget before release |
| Growth | Estimate events/day × retention days × event size, including indexes, replicas/backups and exports |
| Resource bounds | Paginated queries, bounded import/deletion batches, and streamed or queued large exports |
| Concurrency | Optimistic conflict handling; safe multi-instance scheduled jobs and retry behaviour |
| Operations | Monitor capture failures, export failures, CSA report failures, cleanup backlog and storage growth |
| Recoverability | Demonstrate backup restoration with evidence intact; agree recovery-time and recovery-point objectives |
| Accessibility | Keyboard-operable controls, labelled inputs, readable comparisons and explicit state messages |
| Compatibility | Old audit schema versions remain searchable and understandable after application upgrades |

Performance targets are provisional until daily event volume, peak writers, record size, concurrent reviewers, export size and deployment resources are supplied. H2 remains the requested initial database; production suitability and seven-year capacity must be validated rather than assumed. Storage boundaries should allow a later database change, with migration and integration testing.

## 12. Acceptance scenarios

1. **Committed change:** Updating a limit from 10,000 to 15,000 produces exactly one logical applied-change event with those values, the authenticated maker and backend time.
2. **Rollback:** A failed business transaction leaves no successful-change event and no persisted business update.
3. **Audit failure:** Required audit persistence failure prevents the business update from committing.
4. **Approval:** Pending, rejected and approved changes remain distinguishable; original submission evidence is preserved and linked to later decisions.
5. **Concurrent update:** A stale edit cannot overwrite a newer value while producing misleading before/after evidence.
6. **Permissions:** A user cannot discover out-of-scope evidence through search, totals, detail IDs or exports.
7. **Export consistency:** Exported rows match the recorded criteria/cutoff and authorised fields; newly arriving events do not enter the running export.
8. **Import:** A partially successful import identifies its batch, successful changes and failed outcomes without representing failure as applied data.
9. **Administration:** User-role maintenance appears in the correct day's CSA report with administrator identity and before/after state.
10. **No activity versus failure:** A complete empty day produces a null report; a failed generation produces a failure status and operational alert.
11. **Retention boundary:** Events younger than or exactly at the unexpired boundary survive; only expired eligible evidence is deleted. Test leap-day and timezone boundaries explicitly.
12. **Cleanup recovery:** Interrupted and overlapping cleanup executions cause neither early deletion nor partial event/detail removal.
13. **Deleted subject:** An authorised user can still understand evidence after the underlying business record is removed.
14. **Extension:** A second setup type is integrated through the agreed contracts without changes to unrelated modules.

## 13. Delivery sequence

1. **Requirements agreement:** Confirm event catalogue, access matrix, retention rules, daily-report boundary and deployment scale.
2. **One complete workflow:** Implement one static-data setup type with capture, approval evidence, search, detail and rollback/concurrency validation.
3. **Reuse validation:** Add a second setup type and a business activity through the same contracts; resolve gaps before broad adoption.
4. **Reporting coverage:** Add controlled exports, imports, accounting events and daily CSA reporting with null-report handling.
5. **Retention and operational readiness:** Validate dry-run and deletion, multi-instance jobs, backups, permissions and failure monitoring.
6. **Scale validation:** Test representative volumes and confirm production database and capacity choices.

The release is complete only when required event coverage is demonstrated, acceptance scenarios pass, operational ownership is assigned and unresolved release-critical decisions are closed.

## 14. Decisions required before implementation

| Decision | Why it matters |
|---|---|
| Is seven years a minimum or just a default? | Determines whether shorter configuration is valid |
| Does one retention period apply to all three categories? | Determines cleanup policy scope |
| Is H2 required in production or only development/demo? | Determines capacity, recovery and deployment validation |
| Exact roles, scopes and masked fields? | Required for correct search and export authorisation |
| Event/setup catalogue and direct-write paths? | Defines complete audit coverage |
| Feature-specific maker/checker permissions and rejection rules? | Self-approval is prohibited for the six named setup types; remaining permissions need definition |
| Must activation/deactivation also require approval? | Proposed consistent maker/checker control for status changes |
| Fields, unique keys, validations and dependency rules for each setup type? | Defines feature-specific forms and approval-time validation |
| How long should generated files remain downloadable? | Secure download of CSV and Excel (XLSX) is confirmed; artifact expiry is separate from audit retention |
| Daily report timezone, cutoff and retry policy? | Defines reporting-day correctness and distribution |
| Expected volume, concurrency and export size? | Enables credible performance targets and sizing |
| Holds, archives, export expiry and backup lifecycle? | Defines the complete retention and deletion boundary |
| Who owns OS/process logging and monitoring? | Closes the broader attached policy requirement |

## 15. Traceability

- Original static-data acceptance criteria → CAP, APR, SRCH-01, EXP and RET requirements.
- Original business audit acceptance criteria → CAP, SRCH-02/04, EXP, IMP and ACC requirements.
- Attached administrator policy → ADM requirements plus the explicit OS/process scope decision.
- Properties-configurable expiry request → Section 9.
- Reusability, clean structure and scalable quality request → Sections 10–13.


## 16. Static-data maintenance and maker/checker workflow

### 16.1 Feature catalogue

| Setup type | Confirmed functions | Information still required |
|---|---|---|
| Holiday Calendar | Add, Edit, View; Active/Inactive control | Calendar ownership, date rules and applicable markets |
| Physical Gold | Add, Edit, View; Active/Inactive control | Meaning of the setup entity, identifiers and editable attributes |
| Loco Singapore | Add, Edit, View; Active/Inactive control | Entity definition and location-specific attributes |
| Vault | Add, Edit, View; Active/Inactive control | Identifiers, location details and dependencies |
| Fee Schedule | Add, Edit, View; Active/Inactive control | Fee rules, currencies, applicability and effective-date rules |
| Managed Reference Data | Add, Edit, View; Active/Inactive control | Reference categories, allowed values and category-specific validation |

Names are normalised from the user's message. No feature-specific fields or business rules are considered confirmed merely by their appearance in an example or wireframe.

### 16.2 Separate operational status from request status

**Operational status** belongs to the approved record: Active or Inactive.

**Request status** belongs to a proposed change: Pending approval, Approved or Rejected. Draft and Withdrawn are optional future states unless explicitly requested.

An approved record can remain Active while an edit is Pending approval. A pending addition has no approved operational record yet. Do not use Inactive as a substitute for Pending approval.

The proposed initial policy is that a newly approved addition becomes Active. Confirm whether the maker may instead request an initially Inactive record or a future effective date.

### 16.3 Add workflow

1. A maker with add permission enters the feature-specific details.
2. Submit validates the details and stores a pending change request, proposed values, maker identity and audit evidence together.
3. The request is visible to authorised reviewers but is unavailable as approved operational data.
4. A different user with approval permission for that feature and scope reviews the proposed values and reason.
5. Approval revalidates permissions, uniqueness and business constraints, then creates the approved record, completes the request and writes approval/application evidence in one transaction.
6. Rejection preserves the proposal and decision evidence and creates no operational record.

### 16.4 Edit workflow

1. The maker opens an approved record and edits permitted fields.
2. Submit stores the baseline record version, current-value snapshot and proposed values in a pending request. It does not update the approved record.
3. The checker sees field-level current versus proposed values, maker, reason and submission time.
4. Approval checks that the record still matches the baseline version and that the proposal remains valid.
5. Successful approval updates the record, completes the request and records the actual before/after evidence atomically.
6. Rejection leaves the approved record unchanged. A revised proposal creates a new linked request; submitted historical evidence is not overwritten.

If permissions have changed, the checker is the maker, or the baseline is stale, approval must not apply the change. The interface explains the failure and requires a fresh eligible decision or proposal as applicable.

### 16.5 Active/Inactive control — proposed approval rule

The table shows current operational status and an action button: Deactivate for Active records, Activate for Inactive records. Selecting the action opens a reason/submission step and requests approval; it does not immediately toggle the operational value.

| Current state | Submitted request | State while pending | State after approval | State after rejection |
|---|---|---|---|---|
| No approved record | Add | Pending addition only | Active, under the proposed default | No approved record |
| Active | Edit | Active; existing values retained | Active; approved values applied | Active; existing values retained |
| Inactive | Edit | Inactive; existing values retained | Inactive; approved values applied | Inactive; existing values retained |
| Active | Deactivate | Active | Inactive | Active |
| Inactive | Activate | Inactive | Active | Inactive |

Whether inactive records can be edited and whether status changes need approval remain proposed rules. Deactivation effects on dependent records and existing transactions must be defined per feature; do not cascade changes or delete historical data implicitly.

### 16.6 Table and review experience

Each setup screen has an Add action, search/filter controls, and a table containing the identifier, name/summary, operational status, pending-request indicator, last approved change time and permitted actions.

- View opens the approved data and links to its audit history.
- Edit starts a proposal against the approved record.
- Activate/Deactivate starts the proposed approval-controlled status request.
- View pending change opens the current/proposed comparison.
- Pending additions appear in a clearly labelled Pending approvals view rather than appearing as usable operational records.
- A shared Pending approvals view can filter by setup type, request type, maker and submission date.
- Checker detail shows Approve and Reject only when permitted; the backend independently enforces permissions and distinct maker/checker identities.
- Proposed rule: require a reason for edit, status-change and rejection actions.

Proposed concurrency policy: allow one pending change per existing record at a time. Disable competing edit/status submissions with an explanation and enforce that restriction atomically in the backend. Define a unique-business-key rule for competing pending additions. This policy is configurable design scope only if concurrent proposals are later required; it is not an assumed feature.

### 16.7 Reuse and ownership

Use a shared change-request lifecycle for submission, reviewer eligibility, decision handling, audit linkage and review presentation. Each feature owns its fields, validation, unique keys, snapshot mapping and application of an approved proposal.

The workflow capability and audit capability have separate ownership: workflow manages pending work and decisions; audit preserves evidence. An operational change request may transition state, while its audit events remain append-only until authorised expiry.

Adding a seventh feature should require registering its feature rules and proposal representation, not copying an entire approval service. Keep typed feature-specific validation even when storage or UI rendering shares common metadata.

### 16.8 Additional acceptance criteria

| ID | Scenario and expected result |
|---|---|
| SET-01 | All six named features support authorised Add, Edit and View operations. |
| SET-02 | Submitting an addition produces a pending request and evidence, with no usable approved record. |
| SET-03 | A different, feature-authorised checker can approve the addition; exactly one approved record is created. |
| SET-04 | The maker cannot approve their own request even if they otherwise have checker permission or call the API directly. |
| SET-05 | A submitted edit leaves approved data unchanged until approval; approval applies exactly the reviewed valid proposal. |
| SET-06 | Rejected additions/edits preserve the proposal and rejection evidence without changing operational data. |
| SET-07 | Tables show operational status separately from pending approval status. |
| SET-08 | Under the proposed status policy, Activate/Deactivate does not change status until approval by another authorised user. |
| SET-09 | Stale, duplicate and simultaneous approval attempts cannot apply a change twice or apply an outdated proposal. |
| SET-10 | Approval application, request completion and required audit writes succeed together or all roll back. |
| SET-11 | Feature and scope permissions apply independently to View, Add, Edit, status requests and approval. |
| SET-12 | Approval revalidates current constraints, including uniqueness and dependencies; failures are explained without falsely recording success. |
| SET-13 | The audit report links submission, decision and actual application, preserving maker, checker, timestamps, reasons and field values. |

These scenarios apply to all six features. Status-control behaviour, initial operational status, rejection reasons and the single-pending-request policy are proposed defaults requiring business confirmation before implementation.


## 17. Dynamic user-group access and audited administration

### 17.1 Confirmed direction

Users belong to user groups, such as Bullion Clearing. Different groups receive different feature, data and reporting access. User/group mappings can change over time and those changes must themselves produce audit evidence. Reports support CSV and Excel (XLSX), generated on user request or by a scheduler.

Group names must not be hard-coded into business services. The model should support multiple memberships per user, even if the initial deployment assigns one group per user. Multiple-membership semantics below are proposed until confirmed.

### 17.2 Recommended access model

Use group-based permissions with data-scope constraints. A permission answers **what action is allowed**; its attached scope answers **on which records**. Keep operational status, approval state, group membership and permission grants separate.

| Concept | Purpose |
|---|---|
| User | Stable identity, current status and authentication identity mapping |
| User group | Stable group ID, display name and active/inactive status |
| Membership | User-to-group relationship, validity and change history |
| Permission grant | Feature or report key, allowed action, data scope and field policy |
| Scope | Explicit allowed organisation, account, business area or record population |
| Policy version | Identifies the policy evaluated for a decision or generated report |

Possible actions include View, Add, Edit, Request activation/deactivation, Approve, Search audit, Generate report, Download report, Send report, Manage schedule and Manage membership. These are separate grants: membership alone must not automatically confer every action.

A user in Bullion Clearing receives only its configured grants. The group name does not determine record ownership; records and grants need explicit scope information. Feature permission alone must not permit every account or organisation's data.

**Proposed evaluation rules:**

1. Require an active authenticated user and active applicable membership/group.
2. Match the requested action and registered feature/report key to an applicable grant.
3. Evaluate that grant's attached data scope and field restrictions together.
4. Combine applicable allow-grants without mixing an action from one grant with an unrelated scope from another.
5. Apply mandatory restrictions, including maker/checker separation and sensitive-field rules.
6. Deny when no applicable grant exists. New features receive no permissions automatically.

For example, View for scope A plus Download for scope B must not yield Download for scope A. If multiple grants impose overlapping field restrictions, the proposed rule is to apply the most restrictive applicable field policy. Final combination rules must be approved and tested.

Use one shared access-policy capability across business services, audit search, detail, counts, reports and downloads. UI controls reflect permissions for usability; backend operations independently enforce them. Scoped database queries must exclude unauthorised evidence before pagination and counts.

### 17.3 Membership and policy administration

Capture additions, removals, moves between groups, group activation/deactivation and changes to permission/scope grants. Evidence includes the target user/group stable IDs, before/after mapping or grant, requesting administrator, reason, decision identities when applicable and timestamps.

Group display-name changes must not break historical evidence. Preserve event-time identity and scope while authorising access against current policy. Former membership must not continue granting access; membership at the time of a historical event does not by itself determine who may view that event today.

**Proposed control:** Membership and permission changes use the shared maker/checker process, with a different administrator approving them. A pending membership proposal has no effect until approved. Self-approval and privilege escalation through self-service mapping are prohibited. Whether all security administration needs this dual control remains a business decision.

Permission decisions must use current committed policy. If caching is introduced, revocations require invalidation/version checking and a documented enforcement bound; a stale session must not be the sole authority. New protected requests after committed revocation must not rely on an obsolete grant. Define handling of already-running operations separately.

These administration events appear in administrator-activity reporting. Authentication tokens, passwords and secrets must never appear in before/after evidence.

## 18. Shared report generation and distribution

### 18.1 One pipeline for manual and scheduled requests

Both entry points use the same report-definition registry, authorisation, filtering, field projection, formatting, storage and lifecycle capabilities. A scheduler must not bypass the access matrix.

The conceptual sequence is:

Request or scheduled occurrence → validate authority and criteria → establish scope and cutoff → generate permitted rows → produce CSV/XLSX → store protected output → mark ready → authorised download or configured delivery.

Each report definition declares its stable key, supported filters, controlled fields, query provider, supported formats and permitted audience rules. The six setup features reuse the generic static-data report where their evidence follows the common contract. A new specialised report supplies its own provider without copying the job/export machinery.

### 18.2 Lifecycle and reproducibility

Track Requested, Running, Ready, Failed and Expired. Cancelled may be added if cancellation is required. Use distinct delivery outcomes rather than conflating a Ready file with successful distribution.

Persist report-run ID, report definition/version, trigger type, requester or schedule identity, effective audience/scope, criteria, selected fields, format, timezone, source cutoff, evaluated policy version, timestamps, row count and protected artifact reference. Proposed integrity metadata includes output size and a checksum.

An artifact represents the criteria and permissions at generation time. A policy snapshot explains what happened; it is not a permanent access grant. Adding privileges later must not silently add rows to an existing file.

A failed or partial output is not available as a successful report. CSV and XLSX must carry equivalent authorised data; document format-specific representation of criteria and metadata. Large reports use bounded reads and background jobs. Oversized Excel output must follow an explicit split-or-reject rule rather than silently truncate records.

### 18.3 Download and permission changes

Re-evaluate current access for every protected download. If an existing file contains rows or fields no longer permitted to that user, deny that file and offer regeneration with current permissions; do not simply check that the user still belongs to any group.

Stored outputs require access controls and configurable expiry separate from raw audit retention. Do not expose public artifact URLs. If temporary links are introduced later, their revocation behaviour must meet the agreed access policy.

Record generated, failed and observed download events separately. Record an observed download request/response accurately; do not claim that the recipient read the report or saved every byte. Previously downloaded copies cannot be recalled by changing group membership, which is relevant when selecting a distribution channel.

### 18.4 Scheduled reporting

A schedule defines report key, criteria/date-window rule, format, timezone, frequency, owner, intended audience, delivery configuration and enabled status. Schedule changes themselves are audited.

Proposed execution modes:

- **User-owned schedule:** Validate that the owner remains active and authorised on every run. Suspend/fail visibly if authority is removed.
- **Group-owned schedule:** Use a dedicated service identity constrained to the report's approved group scope. Resolve eligible recipients from current group policy for each run and again before delivery/access.

Do not give the scheduler blanket access because it is automated. Do not create a broad report and assume every member of a target group may see it. For audiences with different row/field permissions, generate separate authorised outputs or an explicitly defined common permitted scope.

Use a stable occurrence identity to prevent duplicate report runs across scheduler instances and retries. Track generation and delivery retries separately. Treat external delivery with an unknown outcome as uncertain; do not promise exactly-once delivery without channel support.

The CSA daily report is one schedule definition with its required audience and null-report behaviour. It reuses the same reporting pipeline rather than a separate implementation.

### 18.5 Confirmed post-generation flow: secure download

After successful generation, both manual and scheduled reports appear in a protected Reports area with a Download action for eligible users. CSV and Excel (XLSX) are supported. Email, external sending and notifications are not part of the confirmed delivery flow; delivery adapters remain a possible future extension.

The Reports list shows only authorised report entries and includes report name/type, format, trigger (manual/scheduled), requester or owning group, generation time, status, row count when available and expiry time. Running, Failed and Expired reports have no usable Download action. A Ready report becomes downloadable only after the complete output has been stored successfully.

Every download requires authentication and a current authorisation check covering the report and its entire row/field scope. Report-list visibility, guessed report IDs and direct artifact requests must follow the same policy. Files must not be publicly accessible or exposed by unprotected storage paths. If the user's permissions no longer cover an existing artifact, deny download and offer authorised regeneration.

Record observed download requests and outcomes with user identity, report-run ID and timestamp. Do not equate a download response with proof that the user read or retained the file. Expiry removes download access, and a controlled cleanup process removes the generated artifact under its separate configured lifecycle.

Proposed audience default: manual reports are accessible to their authorised requester; group-owned scheduled reports are accessible to currently eligible users in the configured owning/audience group. Sharing a manual report with other users is not automatic. Final audience grants remain part of the access-matrix decision.

## 19. Maintainable capability boundaries

| Capability | Reusable responsibility | Feature-specific extension |
|---|---|---|
| Identity/access | Membership resolution, permission and scope evaluation | Registered feature/action/scope definitions |
| Change workflow | Submit, distinct checker, decisions and request concurrency | Validation and application of each proposed change |
| Audit | Context, immutable evidence, comparison and search contracts | Allowlisted snapshots and business event meanings |
| Report engine | Jobs, cutoffs, policy checks, progress and output lifecycle | Report query/provider and column definitions |
| Format adapter | CSV/XLSX rendering and safe value encoding | Formatting metadata where required |
| Scheduler | Due-run dispatch, occurrence identity and retries | Schedule definition and approved audience |
| Delivery adapter | Protected distribution and outcome tracking | Optional approved channel |
| Retention | Policy evaluation and bounded cleanup | Separate evidence/artifact policies |

These are ownership boundaries, not a requirement to create eight deployable services or a class for every row. Start with a modular monolith and keep transactional operations local. Business modules depend on small contracts; adapters isolate persistence, formats and future delivery channels.

Use Strategy for feature validation/report providers, adapters for formats and delivery, and a shared policy component for authorisation. Keep transaction coordination explicit. Prefer composition, cohesive responsibilities and small interfaces over inheritance trees and feature-name conditional chains.

Configuration governs memberships, grants, schedules and retention. A completely new domain feature will still need its own fields, rules and registration; “dynamic” does not mean arbitrary new business behaviour can be added without implementation or validation.

### Adding a seventh feature

1. Define its stable feature key, data scope, fields and business validation.
2. Implement its proposal/snapshot mapping and approved-change application through existing contracts.
3. Register its actions and audit labels/schema version.
4. Reuse the generic report or provide a specialised report definition where semantics differ.
5. Assign explicit grants to the intended groups; deny access until assigned.
6. Verify maker/checker, audit, report/export and cross-group isolation using the same acceptance contract.

Existing feature workflows, CSV/XLSX renderers and scheduler logic should not need to be copied or edited solely to add the feature.

## 20. Additional acceptance criteria

| ID | Scenario and expected result |
|---|---|
| GRP-01 | A Bullion Clearing user can perform only configured actions on its permitted records and fields. |
| GRP-02 | A user from a different group cannot access that scope through UI, direct API, report ID, counts or artifact reference without an applicable grant. |
| GRP-03 | Multiple memberships never combine unrelated action/scope pairs into broader access. |
| GRP-04 | Committed membership/grant changes affect subsequent authorisation and produce before/after audit evidence. |
| GRP-05 | Under the proposed approval rule, pending mapping changes grant no access and cannot be self-approved. |
| GRP-06 | Group rename/removal does not erase or corrupt historical membership evidence. |
| RPT-01 | Equivalent manual and scheduled requests with the same scope, cutoff and criteria produce equivalent authorised rows and fields. |
| RPT-02 | CSV and XLSX exports preserve controlled values and criteria without silent truncation. |
| RPT-03 | Revoked access prevents protected downloads; existing overbroad artifacts are denied rather than released. |
| RPT-04 | A user-owned schedule loses authority when its owner loses the necessary permission. |
| RPT-05 | A group report is not distributed to members who lack access to its complete contents. |
| RPT-06 | Retry and concurrent scheduler execution do not produce duplicate logical occurrences; uncertain external delivery is visible. |
| RPT-07 | Generation, failure, expiry, download and any eventual delivery outcomes are distinguishable and auditable. |
| RPT-08 | Successfully generated manual and scheduled reports appear in the protected Reports area and permit secure CSV/XLSX download only to currently authorised users. |
| RPT-09 | Running, failed, expired or unauthorised artifacts cannot be downloaded through either the UI or a direct request. |
| EXT-01 | A seventh feature integrates through registered contracts without duplicating approval, access, format or scheduler implementations. |

### Decisions added in this revision

- Can a user belong to multiple groups, and are the proposed allow-grant/field-restriction combination rules accepted?
- Who may maintain groups, memberships, permissions and schedules, and which changes need maker/checker approval?
- Which data-scope dimensions distinguish groups and individual users?
- What is the generated-file expiry period, and are the proposed manual/scheduled report audience defaults accepted? Secure download is confirmed.
- Are scheduled reports user-owned, group-owned or both? What are the artifact expiry and permitted in-flight revocation rules?


## 21. Technical quality and coding standards

### 21.1 Status and intent

**Confirmed:** The implementation must use clean, maintainable code, application-wide exception handling, explicit enums where appropriate, positive and negative scenario testing, SOLID principles, deliberate design patterns, clear module boundaries and a common `cc_` entity-related naming prefix.

**Naming interpretation requiring confirmation:** Apply `cc_` to application-owned database tables and database object names; use conventional PascalCase Java entity class names. If the user intends the prefix on Java classes as well, settle that convention before implementation. This document contains requirements and conceptual names, not application code.

### 21.2 Naming and persistence conventions

| Element | Required convention / proposed examples |
|---|---|
| Application-owned table | Lowercase snake_case with `cc_`: `cc_vault`, `cc_fee_schedule`, `cc_audit_event`, `cc_user_group` |
| Join table | Same prefix and naming rule, such as `cc_user_group_membership` |
| Java entity class | PascalCase business name, such as Vault or AuditEvent, mapped explicitly to its table |
| Java package | Lowercase, organised by capability/module and responsibility |
| Method and variable | Descriptive camelCase; avoid unexplained abbreviations and generic names |
| Column | Lowercase snake_case; explicit stable mapping |
| Constraint/index | Deterministic names incorporating `cc_`, object role and table, subject to database length limits |
| Enum type/value | PascalCase type, UPPER_SNAKE_CASE constants, stable external/persisted codes |
| Error code | Stable namespaced or prefixed business code, independent of exception class name and display message |

The database prefix is a naming convention, not an access-control mechanism. Tool-managed metadata tables may follow their tool's required naming rules.

Use versioned schema migrations and reviewed constraints/indexes. Do not rely on automatic destructive schema generation in deployed environments. Database changes must preserve historical audit interpretation and be tested against the actual deployment database; H2-only tests do not establish compatibility with a different database selected later.

### 21.3 Module and layer rules

- Business modules own their business entities, validation and feature-specific operations.
- Controllers/adapters handle transport mapping and delegate to application use cases; they do not implement business workflows or access repositories directly.
- Application services coordinate authorisation, validation, transactions, workflow and audit recording.
- Domain rules express allowed transitions and invariants without depending on HTTP response handling or export formatting.
- Persistence adapters own JPA/database concerns. API DTOs are separate from persistence entities; do not return entities directly as public API contracts.
- Shared contracts remain small and stable. Avoid a general-purpose common module that accumulates unrelated business behaviour.
- Modules must not directly use another module's repositories or internal entities. Cross-module operations use declared contracts.
- Dependencies must be acyclic. Architecture checks enforce these boundaries in continuous integration.
- Use composition for shared behaviour; a common auditable base entity/service must not become the mechanism for inheriting unrelated workflow, security and reporting concerns.

### 21.4 SOLID and pattern application

| Principle/pattern | Concrete requirement |
|---|---|
| Single Responsibility | Keep request handling, business rules, access decisions, evidence writing, reporting and error translation cohesive and separate |
| Open/Closed | Extend feature validation and report providers through registered contracts rather than adding feature-name branches throughout shared services |
| Liskov Substitution | All implementations honour the same contract, including transaction, error, nullability and durability guarantees |
| Interface Segregation | Separate audit writing from querying; separate report formatting from scheduling and delivery |
| Dependency Inversion | Application use cases depend on capability contracts; persistence and transport adapters implement boundary details |
| Strategy | Select feature validation/snapshot mapping and report format behaviour where implementations actually vary |
| Adapter | Isolate database, CSV/XLSX and any later external integrations |
| Explicit state transitions | Centralise permitted workflow/report transitions; a State-pattern class hierarchy is optional and only justified by complexity |
| Registry | Resolve registered feature/report keys with duplicate and unknown-key validation |

Do not introduce an interface for every class or a design pattern solely to claim compliance. Prefer the smallest structure that preserves a useful extension boundary. New features must not require copying approval, audit, permission, export or exception-mapping infrastructure.

### 21.5 Enum and dynamic-value policy

Use enums for bounded application-controlled concepts, for example operational status, change-request status, change action, report-run status, report format and trigger type. Keep approval status, operational status and report status as distinct types.

Do not use enums for user names, user-group names, memberships, permission assignments or other administrator-managed reference values. Those remain dynamic records with stable identifiers. Feature/report registries may use stable extensible keys so a new feature does not require changing a shared enum used by every module.

Persist stable string codes rather than ordinal positions. Public and historical codes must not depend on enum declaration order. Validate unknown incoming codes with a controlled error; do not silently map them to a permissive default. Define compatibility and migration rules for renamed, removed or newly introduced codes. Historical readers must continue displaying older evidence and must not change security decisions because of an unknown value.

### 21.6 Other implementation standards

- Use constructor-based dependency injection and explicit dependencies.
- Use typed DTOs/value objects for feature input; arbitrary maps must not replace typed domain validation.
- Use UTC instants for event timestamps and an injectable clock for deterministic time tests. Apply user timezone only at reporting boundaries.
- Use appropriate decimal precision and explicit rounding rules for monetary/rate values; avoid binary floating point for these domain values.
- Validate configuration at startup, including retention periods, schedules and bounded job settings.
- Keep transactions around database use cases; avoid holding a database transaction open during long file generation or external I/O.
- Use optimistic version checks and database constraints to enforce invariants under concurrency, not just UI validation.
- Document ownership, transaction boundaries and non-obvious business decisions; avoid comments that merely repeat the code.
- Keep secrets out of source, exceptions, audit values and logs.

## 22. Application-wide exception and failure handling

### 22.1 Failure model

Use a small, meaningful exception taxonomy with stable error codes. Distinct business scenarios may share an exception category while retaining their own codes. Do not create one exception class for every field or use a generic exception for all failures.

Successful operations use normal return/results; exceptions represent failed operations. Empty search results are a successful empty response. Expected pending approval is a normal state, not an exception.

Translate domain/application failures at boundaries. Central HTTP handling covers controller/application exceptions; authentication and security-filter failures must be normalised through their own boundary handlers. Background workers, schedulers and streaming downloads require explicit failure handling beyond controller advice.

### 22.2 Proposed error contract

Use a consistent problem-details-style API response with status, stable error code, safe summary/detail, request/trace ID and structured field errors when relevant. Do not return stack traces, SQL, filesystem paths, tokens or sensitive field values. The frontend uses codes to choose messages/actions rather than parsing exception strings.

Protected missing or out-of-scope objects use a consistent non-disclosing response. Keep the actual cause in restricted operational logs. Error descriptions and IDs must not leak another group's data.

### 22.3 Scenario catalogue

The HTTP mappings below are proposed API standards; workers use the same codes with their own job-state handling.

| Scenario | Error category / example code | Proposed response and required behaviour |
|---|---|---|
| Malformed input, invalid enum, invalid date range | Validation / feature code or `SYS-400-001` at the framework boundary | 400; safe field errors; no business write |
| Missing/expired authentication | Authentication / `IDN-401-001` | 401; no protected operation |
| Authenticated user lacks an action grant | Access / feature code such as `IDN-403-001` | 403; no operation; do not expose restricted data |
| Missing or inaccessible protected record/report | Lookup / feature code such as `WF-404-001` or `RPT-404-001` | 404; consistent response prevents existence disclosure |
| Duplicate record key or competing pending request | Conflict / `WF-409-001` or `WF-409-002` | 409; preserve existing data |
| Stale record version | Conflict / `WF-409-003` | 409; require refresh and a valid new proposal |
| Self-approval attempt | Access / `WF-403-001` | 403; preserve pending request and approved data |
| Already-decided request or invalid transition | Conflict / `WF-409-004` | 409, or prior result for a recognised idempotent replay |
| Valid syntax but invalid feature rule/dependency | Business rule / `WF-422-001` | 422; actionable safe explanation; no application |
| Unsupported report format | Validation / REPORT_FORMAT_UNSUPPORTED | 400; no report job created |
| Report still running | Conflict / `RPT-409-001` | 409; preserve job; allow status inspection |
| Expired report, where caller may know its existence | Lifecycle / `RPT-410-001` | 410; no file; offer permitted regeneration |
| Revoked report scope | Access / REPORT_ACCESS_REVOKED | 403 or non-disclosing 404 per protected-resource policy; no file |
| Required audit/database storage unavailable | Infrastructure / `SYS-503-001` | 503 when temporary unavailability is established; roll back required business writes |
| Known domain constraint detected by database | Conflict/business rule / specific stable code | Map the recognised constraint; do not mislabel every database failure as a conflict |
| Unexpected programming or unmapped persistence failure | Internal / `SYS-500-001` | 500; safe response, correlated restricted log and alert as appropriate |
| Export worker/storage failure | Job / REPORT_GENERATION_FAILED | Mark job failed or retrying; never expose partial output as Ready |
| Download fails after streaming starts | Transport / DOWNLOAD_INTERRUPTED | Abort stream and log outcome; do not try to send a second JSON response or claim success |
| Invalid retention configuration | Configuration / RETENTION_CONFIGURATION_INVALID | Fail startup/configuration validation; destructive cleanup must not run |
| Retention batch failure | Job / RETENTION_BATCH_FAILED | Roll back affected batch, preserve completed batches and record resumable failure |
| Scheduled owner no longer eligible | Access / SCHEDULE_AUTHORITY_REVOKED | Skip/suspend with visible failure; do not run with expanded authority |

### 22.4 Transaction, retry and operational rules

- Never swallow required audit failures or return success after rollback.
- Preserve useful exception causes internally; translate once at the responsible boundary and avoid duplicate stack-trace logging at every layer.
- Retry only classified transient failures with bounded attempts and backoff. Do not retry validation, authorisation, stale-proposal or rejected-business-rule failures automatically.
- A failed transaction must be retried through a fresh transaction boundary, not reused after being marked for rollback.
- Use idempotency/occurrence keys and concurrency guards before retrying operations with side effects. A network timeout after commit does not prove that the operation failed.
- Record background-job failure state durably where possible; if the database is unavailable, emit restricted operational telemetry and reconcile job state after recovery. Do not claim guaranteed database failure recording while that database is down.
- Failed-attempt activity is separate from committed-change evidence. Its recording path must not falsely create an applied-change event.
- Every failure is traceable to a request/job/report ID. Log severity distinguishes expected client/business failures from unexpected service failures.
- Alert on operationally significant failures and exhausted retries without exposing confidential input.

## 23. Positive, negative and boundary test strategy

### 23.1 Required coverage by scenario

Maintain a requirement-to-test matrix linking each applicable PRD ID to positive, negative, boundary and concurrency scenarios. Not every requirement has all four categories; document legitimate non-applicability. A line-coverage number is not proof of business correctness.

| Capability | Positive scenarios | Negative/boundary/concurrency scenarios |
|---|---|---|
| Add | Valid submission and different authorised checker creates one record | Invalid fields, duplicate key, self-approval, insufficient scope, duplicate submit/approval |
| Edit | Approved proposal applies the reviewed differences | Rejection, stale version, no-op proposal policy, concurrent edit, dependency change before approval |
| Activate/deactivate | Approved status transition under the agreed policy | Invalid transition, pending conflict, dependency restriction, unauthorised checker |
| Access/groups | Correct scoped grants; approved mapping change takes effect | Cross-group IDs/counts/files, expired membership, revoked grants, unsafe multi-group union, stale permission cache |
| Audit | Correct actor, reason, values and linked decisions | Audit failure rollback, business rollback, missing context, duplicate delivery, secret leakage, deleted subject |
| Search | Accurate filters, deterministic pagination and empty success | Invalid dates, timezone boundary, tied timestamps, unauthorised totals/detail |
| CSV/XLSX | Equivalent authorised values and recorded criteria | Formula injection, quoting/newlines, Unicode, size limit, field masking, interrupted output |
| Reports/download | Ready manual/scheduled file securely downloads | Pending/failed/expired file, revoked access, guessed ID, partial storage, streaming failure |
| Scheduler | One eligible occurrence; correct CSA null report | Overlapping instances, retry, lost owner permission, incomplete day, generation failure incorrectly labelled empty |
| Retention | Eligible evidence deleted atomically with details | Exact cutoff/leap day, invalid config, active holds if adopted, overlapping jobs, interrupted batch, distinct artifact expiry |
| Exceptions | Known failure maps to its expected safe response | Unknown failure maps safely, security-filter errors normalised, no stack/SQL/secret leak |
| Extension | A second/new feature passes shared contracts | Duplicate/unknown registry key, invalid implementation contract, historical enum/schema compatibility |

### 23.2 Test layers

1. **Unit tests:** Comparison rules, state transitions, permission combination, feature validation, expiry calculations and error classification using deterministic clocks.
2. **Contract tests:** Reusable expectations for feature handlers, audit writers and report formats, including failure semantics.
3. **Integration tests:** Real transaction commit/rollback, JPA mappings, constraints, optimistic locking, migrations and job persistence. Avoid mocking the database for transactional guarantees.
4. **API/security tests:** Validation, authentication, scope filtering, direct-resource access and standard error responses.
5. **End-to-end acceptance tests:** Representative maker/checker, reporting/download and group-change journeys across all six features' critical rules.
6. **Resilience/concurrency tests:** Audit/storage failures, retries after uncertain outcomes, duplicate approvals and multi-instance jobs.
7. **Performance tests:** Representative agreed data volume, query distributions, export sizes and retention backlog; verify declared service targets.

Use isolated repeatable fixtures and synthetic data. Tests must assert domain outcomes and protected data, not mirror implementation details. Avoid arbitrary sleeps; control clocks and coordinate concurrency deterministically where possible.

### 23.3 Continuous integration and definition of done

Every implementation change must pass the applicable build, formatting, static analysis, architecture rules and relevant automated tests. Select and pin the tooling during technical design. Review security/dependency findings under an explicit severity policy; exceptions require recorded ownership and rationale.

A feature is complete when:

- Its approved requirements and positive/negative scenarios are traceable to passing tests.
- Required audit, transaction and access guarantees hold under failure and concurrency.
- Stable error codes, safe messages and correct boundary handling are documented and verified.
- Naming, enum, module and migration conventions are followed.
- New extension behaviour is registered without copying shared workflows or introducing cross-module internals.
- Relevant API/configuration/operational documentation is updated.
- Performance targets are met where the change affects them, and no unresolved critical correctness/security defect remains.

Coverage thresholds may be agreed as supporting gates; do not invent a percentage as a substitute for the scenario matrix. Require branch/failure-path coverage review for critical authorisation, approval, audit atomicity and retention logic.

## 24. Technical acceptance and remaining conventions

| ID | Requirement |
|---|---|
| TECH-01 | Application-owned tables use `cc_` naming; Java class naming follows the explicitly agreed interpretation. |
| TECH-02 | Module architecture checks prevent cycles and access to another module's internal repositories/entities. |
| TECH-03 | Bounded lifecycle concepts use distinct enums/stable codes; dynamic groups and grants remain data-driven. |
| TECH-04 | HTTP, security, jobs and streaming boundaries have tested failure handling appropriate to their execution model. |
| TECH-05 | Every known scenario maps to a documented result/error code; unexpected failures produce safe responses and correlated diagnostics. |
| TECH-06 | Positive and negative acceptance tests cover each applicable capability; boundary/concurrency tests prove critical invariants. |
| TECH-07 | Required audit failure cannot leave committed business changes; retries cannot duplicate application of an approved proposal. |
| TECH-08 | New feature/report implementations pass shared contracts without copying infrastructure. |
| TECH-09 | Formatting, static analysis, architecture and applicable automated checks pass before integration. |

Decisions to close during technical design: exact interpretation of the entity prefix; concrete error-code catalogue and API status policy; validation/no-op proposal rules; selected quality tools; production-database test target; and any supporting coverage threshold. These choices must preserve the confirmed functional and integrity requirements above.


## 25. Centralised constants, API routes, configuration and messages

### 25.1 Requirement and ownership rule

**Confirmed:** Shared constants, API endpoint mappings and error messages must have clear central definitions so changes can be made consistently in one place.

Centralisation means one authoritative definition per concern, not one global file containing every value in the application. Shared definitions belong to a small common contract/configuration area; feature-specific constants belong to their owning module. Avoid circular dependencies and a growing unrelated Constants class.

Ordinary local variables, method parameters, per-request values and user-specific state must remain scoped to their operation. Do not centralise mutable runtime state in static/global variables, which would create concurrency and data-isolation risks.

### 25.2 Definition catalogue

| Concern | Authoritative location / conceptual name | Rules |
|---|---|---|
| API base/version and common route segments | API route contract / ApiRoutes | Centralise shared paths; use consistent versioning and naming |
| Feature endpoint mappings | Feature-owned route definitions registered in the API contract | Do not repeat literal paths across controllers; keep dependencies one-way |
| Shared fixed technical constants | Small concern-specific constants definitions | Immutable, descriptive names; avoid duplicate magic strings/numbers |
| Lifecycle values | Dedicated enums | Reuse Section 21 enum rules; do not duplicate enum values in constants |
| Operational settings | Typed configuration mapped from application properties | Retention, batch limits, scheduling and file expiry have one validated source |
| Error identity | Feature-owned ErrorDefinition catalogue | Stable custom machine-readable codes; independent of message wording |
| Error display text | Central message catalogue / message resource | Clear safe templates; supports future localisation without changing error codes |
| Dynamic memberships, grants and reference values | Their persisted administration model | Never compile user/group names or editable mappings into constants |
| Feature-specific fixed values | Owning feature module | Share only when meaning and ownership are truly common |
| Secrets/environment credentials | Protected deployment configuration | Never hard-code in source constants or message resources |

Conceptual names above are design guidance, not prescribed class implementations. Use UPPER_SNAKE_CASE for Java constants, and avoid duplicating the same setting in constants, properties and database configuration without an explicit precedence rule.

### 25.3 Centralised API mapping and documentation

Maintain a versioned API contract as the authoritative endpoint inventory. Each operation records its HTTP method, route, action permission, request/response contract, validation, expected success status and known error codes.

The route catalogue must distinguish feature maintenance, change requests/decisions, audit search/detail, report requests/status/downloads and administration. Reuse consistent resource naming and path parameters. Centralising routes does not replace permission checks on each operation.

OpenAPI documentation and route mappings must agree; automated checks must detect duplicate/conflicting mappings and undocumented public operations. If a separate frontend consumes the API, generate or validate its client against the contract rather than manually duplicating URLs in many screens.

Adding a seventh feature extends its declared route/contract registration and permissions without editing unrelated controllers. Public route changes require an explicit compatibility/versioning decision; renaming a constant alone must not silently break clients.

### 25.4 Configuration and variable scope

- Keep adjustable values in application properties with typed, validated configuration objects. Do not scatter property lookups across business services.
- Group properties by capability, such as audit retention, report generation and scheduler behaviour.
- Define defaults and allowed ranges once; document environment override precedence and restart/reload behaviour.
- Keep actual users, memberships and grants dynamic as already required; properties are not the administration database.
- Derive request-specific values such as the current actor, scope, correlation ID and cutoff from the current operation. Pass them explicitly or through a safely scoped context; never share them across users through mutable globals.
- Centralise values because they share meaning, not because identical literals happen to appear in unrelated business rules.

### 25.5 Clear exception messages and handling

Every documented failure has a stable error code and a centrally maintained safe message template. Boundary handlers resolve the code and approved parameters into the response; business services must not construct inconsistent user-facing messages independently.

A message should explain what failed and, when possible, the user's next action. Avoid generic “Something went wrong” for known failures. Do not expose stack traces, SQL, internal paths, sensitive values or another group's resource existence.

| Scenario | Example user-facing message |
|---|---|
| Invalid date range | “From date must be on or before To date.” |
| Stale proposal | “This record changed after your request was submitted. Refresh the record and submit a new request.” |
| Self-approval | “You cannot approve your own request. Another authorised user must review it.” |
| Competing pending request | “This record already has a change awaiting approval. Review the pending request before submitting another.” |
| Report still running | “Your report is still being generated. Check its status before downloading.” |
| Expired authorised report | “This report has expired. Generate a new report to download it.” |
| Unavailable protected resource | “The requested resource is not available.” |
| Known temporary service failure | “The service is temporarily unavailable. Try again later. Reference: {traceId}.” |
| Unexpected failure | “We could not complete the request. Contact support with reference {traceId}.” |

Use precise wording about outcomes: do not claim that a business operation failed to commit merely because its response timed out. Uncertain outcomes must use status lookup or an idempotent retry path. Report failures must not imply that a file was generated successfully.

The central message resolver must have a safe fallback for missing codes/templates, and log the catalogue defect without exposing implementation details. Template parameters must be allowlisted and safely encoded by the presentation layer. Message wording may change without breaking clients, because clients use stable error codes.

Validation field errors, authentication/security handlers, application exception handlers and background job summaries must use consistent code/message conventions. Operational logs retain restricted diagnostic detail correlated to the safe client reference. No handler may silently swallow a failure or return a successful result for a failed operation.

### 25.6 Acceptance criteria

| ID | Requirement |
|---|---|
| STD-01 | Shared endpoint paths and fixed values have one authoritative definition per concern; unrelated feature values remain module-owned. |
| STD-02 | No user/request-specific mutable state is stored in shared static constants or global variables. |
| STD-03 | Operational properties have documented defaults, precedence and validation and are consumed through typed capability configuration. |
| STD-04 | Every public endpoint has a consistent documented route, permission, response and error contract; duplicate/conflicting mappings fail verification. |
| STD-05 | Every known error code resolves to a safe actionable message; missing-template and unknown-error fallback behaviour is tested. |
| STD-06 | Tests verify codes, statuses, required field errors and absence of leaked data without coupling every business test to exact message wording. Dedicated catalogue tests verify message completeness and placeholders. |
| STD-07 | A new feature reuses shared route conventions and error handling without copying message text or shared configuration. |
| STD-08 | Enums, constants, properties and dynamic database values have distinct ownership and no conflicting source of truth. |


## 26. Request/response DTOs and consistent logging

### 26.1 Explicit DTO contracts

**Confirmed:** Use dedicated request and response DTOs at API boundaries. Never expose JPA entities directly or accept them as API request bodies.

| Contract | Responsibility |
|---|---|
| Create request DTO | Only client-editable fields required to propose an addition |
| Edit request DTO | Permitted proposed values, reason and expected version/reference where applicable |
| Approval/rejection request DTO | Decision-specific allowed input; never a replacement proposal |
| Search request DTO | Typed filters, validated date range, bounded pagination and allowed sorting |
| Report request DTO | Registered report key, permitted criteria and supported format |
| Response DTO | Explicit permitted output fields; distinguish approved data from pending proposals |
| Page/result DTO | Consistent pagination/result metadata without leaking unauthorised counts |
| Error response DTO | The shared safe error contract, stable code, trace ID and field errors |

Prefer operation-specific DTOs over one large reusable DTO whose fields are mostly optional. Share small value/result contracts only where semantics match. Do not create unnecessary DTO layers for purely internal calls.

- Keep actor identity, checker identity, trusted timestamps, effective permissions and internal workflow state server-owned. Never bind client input onto these fields.
- Separate structural validation on input from feature-specific business validation and database constraints.
- Define required, optional, absent and explicit-null semantics, especially for edits; do not let missing fields accidentally clear stored values.
- Use explicit, testable mappings between DTOs, application/domain input and persisted data. Select manual mapping or a mapping tool during technical design; either must prevent mass assignment and accidental field exposure.
- Response mapping follows the field-access policy. Search, detail and reports must not reveal fields merely because they exist on an entity.
- Keep DTOs immutable where practical. Document date/time, enum code, decimal, pagination and compatibility conventions in the API contract.
- Mapping must not trigger uncontrolled lazy loading or broad entity-graph serialisation. Use appropriately scoped projections/reads for reporting.
- Validate the unknown-field policy explicitly: proposed default is to reject unrecognised mutation fields so misspelled inputs do not silently succeed.

DTO tests cover valid mapping, missing/invalid values, prohibited server-owned fields, absent-versus-null behaviour, compatibility and protected-field exclusion.

### 26.2 Centralised logging approach

**Confirmed:** Provide consistent application logging with one shared configuration and policy. Centralise formatting, level configuration, correlation, sensitive-data rules and boundary failure behaviour. Keep meaningful log statements close to the operation that knows what occurred.

**Proposed stack:** A standard Java logging facade, SLF4J, with a Spring Boot-compatible backend such as Logback. Pin versions through the selected supported dependency configuration at implementation time. Do not introduce a custom logger wrapper merely to put every logging call in one class.

The application owns shared logging configuration. Modules use the same facade and conventions with a logger associated with the responsible component. No console printing, uncontrolled stack-trace printing or feature-specific logging configuration copies.

### 26.3 Structured context and safe content

Production logs should use a consistent structured format. Define these fields centrally where applicable:

- UTC timestamp, severity, service/module and event name.
- Trace/correlation ID and request ID.
- Report/job/run ID for asynchronous work.
- Safe operation and outcome classification, duration and stable error code.
- Policy-approved stable actor identifier only where needed; avoid unnecessary personal data.

Generate or validate incoming correlation identifiers. Propagate context to workers explicitly and clear thread-local/MDC context after each operation so reused threads cannot mix users' data. Never store the current user's context in a mutable global logger object.

Use parameterised logging. Do not routinely log complete request/response DTOs, entity dumps, file contents or before/after audit values. Passwords, tokens, credentials, session cookies, protected financial data and unauthorised personal details must not enter logs. Structured encoding and input constraints must prevent log injection through user-controlled text.

Logging policy includes appropriate access controls, storage/rotation and a retention lifecycle separate from immutable business audit and generated files.

### 26.4 Severity, ownership and failure handling

| Level / responsibility | Required use |
|---|---|
| DEBUG | Restricted diagnostic details suitable for controlled environments; no secrets or unrestricted payloads |
| INFO | Meaningful operation/job milestones and successful outcomes where operationally useful; avoid every-method entry/exit noise |
| WARN | Recoverable unusual conditions, exhausted eligibility or operational degradation that warrants attention |
| ERROR | Unexpected failures or failed critical operations with actionable diagnostics |
| Boundary exception handler | Logs an unexpected failure once with its cause and correlation; returns only the safe error DTO |
| Security handler | Records security-relevant outcomes using policy-approved metadata without resource disclosure |
| Worker/scheduler boundary | Records run outcome, retries and terminal failures even though there is no HTTP response |

Expected validation errors and empty results should not generate ERROR stack traces. Do not log the same exception stack at each layer. A layer that rethrows a failure may add context internally but leaves final stack logging to the responsible boundary.

Operational logging and audit evidence are distinct. An ordinary log line does not satisfy the committed audit requirement. Audit persistence failure must still roll back the required business change. Logging sink failure should follow the agreed operational policy without silently losing mandatory audit evidence; avoid recursive logging failures and define bounded buffering/backpressure behaviour.

Where tracing/metrics are introduced, correlate them with logs instead of duplicating full payloads. Monitor report failures, audit persistence failures, scheduler delays and retention backlog with actionable signals.

### 26.5 Acceptance criteria

| ID | Requirement |
|---|---|
| DTO-01 | All public business APIs use explicit request/response DTOs; no JPA entity appears in the public input/output contract. |
| DTO-02 | Input cannot assign authenticated actor, checker, trusted timestamps, permission scope or internal status. |
| DTO-03 | Mapping tests prove validation, field protection and absent/null semantics for feature operations. |
| DTO-04 | Response DTOs and pagination metadata respect current row/field permissions. |
| LOG-01 | Modules use one shared logging configuration/policy and consistent structured fields. |
| LOG-02 | Request, report and scheduled execution retain correlation without context leakage between users or threads. |
| LOG-03 | Tests with synthetic sensitive values verify they are absent from logs, error responses and unintended DTO output. |
| LOG-04 | Unexpected exceptions produce one boundary-owned diagnostic stack and a safe correlated error response/job outcome. |
| LOG-05 | Log levels distinguish expected business outcomes from unexpected service failures; no routine payload dumping or console printing. |
| LOG-06 | Logging-sink failure behaviour and log lifecycle are documented; operational logs never substitute for required audit storage. |

The shared logging configuration, error DTO contract and correlation handling are infrastructure concerns. Feature DTOs and operation-specific log events remain owned by their modules to preserve clarity and extensibility.


## 27. Traceable failures and secure correlation identifiers

### 27.1 Requirement

Every API request must have a correlation/trace ID that appears consistently in the response metadata and related application logs. Error responses also expose this ID in the shared error DTO so a user can provide it to support. Successful responses carry the ID in a standard response header, avoiding unnecessary changes to every business DTO.

For asynchronous operations, a stable report/job ID links the initiating request, queued work and later download requests. Each independently executing request/run can have its own trace ID; preserve causal links rather than reusing one trace forever across unrelated attempts.

### 27.2 Identifier policy

- Use the configured tracing framework's standards-compliant trace generator if distributed tracing is adopted. Do not create a conflicting parallel tracing scheme.
- Otherwise generate a backend correlation ID from at least 128 bits of cryptographically secure randomness, using Java SecureRandom when implementing the generator. Encode it in a fixed safe format, such as 32 lowercase hexadecimal characters.
- Centralise identifier generation behind the shared correlation capability. Feature services must not invent their own random formats.
- Do not derive identifiers from user names, timestamps alone, sequential counters or non-cryptographic random values. Treat uniqueness as probabilistic, not an absolute mathematical guarantee.
- At an untrusted public boundary, generate the authoritative server ID. Accept upstream trace context only under the agreed trusted-gateway/tracing policy and strict format/length validation. Arbitrary client headers must not let users overwrite internal correlation or inject log content.
- The ID is a support reference, not a credential. Knowing a trace/report ID must never grant access to logs, reports or another user's information.

### 27.3 Illustrative failure example — no implementation code

A user attempts to approve their own Vault change request.

| Response field | Example |
|---|---|
| HTTP status | 403 |
| Error code | `WF-403-001` |
| Message | You cannot approve your own request. Another authorised user must review it. |
| Trace ID | 8f13c60a7d924e58b043a976dce120ab |
| Response header | X-Correlation-ID: 8f13c60a7d924e58b043a976dce120ab |

The related restricted operational log contains the same trace ID, event APPROVAL_DENIED, custom error code `WF-403-001`, safe request reference and timestamp. This expected access failure does not need an unexpected-error stack trace. A support operator can locate the event using the trace ID under their log-access permissions.

For an unexpected database failure, the response instead contains a safe message and `SYS-500-001` or the appropriate feature-owned temporary-failure code. The restricted error log contains the same trace ID and diagnostic cause/stack once at the handling boundary. The response never includes SQL or stack traces.

The example ID is illustrative, not a real recorded incident.

### 27.4 Propagation and handling

Create correlation context before authentication and request processing so security failures are also traceable. Apply it to validation errors, known business failures and unexpected exceptions. Responses generated outside the application, such as gateway failures, require gateway-owned correlation and operational integration; do not claim the application can label responses it never processes.

Propagate context across internal calls and explicitly into asynchronous execution. Record report ID, occurrence/run ID and attempt number as appropriate, with the initiating trace reference. Clear context in all completion/failure paths to avoid leakage on pooled threads.

Support tooling searches restricted logs by the reference. Do not expose raw logs or build a public log-lookup endpoint just because the response contains an ID. Log retention and investigation access remain separate controls.

### 27.5 Acceptance criteria

| ID | Requirement |
|---|---|
| TRACE-01 | API errors include a non-empty trace ID matching the relevant response header and log context. |
| TRACE-02 | Authentication, validation, business and unexpected failures all follow the correlation contract. |
| TRACE-03 | Asynchronous failures can be traced from report/run ID to initiating and execution traces without mixing retry attempts. |
| TRACE-04 | Invalid/untrusted incoming IDs cannot override authoritative context or inject log content. |
| TRACE-05 | Concurrent requests and reused worker threads do not leak correlation or user context. |
| TRACE-06 | Identifier generation follows the tracing standard or shared SecureRandom policy; tests validate format and wiring rather than claiming to prove randomness or uniqueness by sampling. |
| TRACE-07 | Support can locate an unexpected error by the response reference while normal users cannot use that reference to retrieve restricted logs. |


## 28. Enforced module packages and service boundaries

**Confirmed implementation standard:** Each Maven module uses explicit responsibility-based packages under its own `com.custody.<module>` namespace. Public API contracts and business use cases must remain distinct from persistence and implementation details. This revision supersedes the initial flat package layout.

| Package | Responsibility |
|---|---|
| `controller` | HTTP adapters, input validation and DTO responses; depend on service contracts |
| `service` | Public use-case interfaces consumed by controllers and other modules |
| `service.impl` | Spring-managed implementations and explicit transaction coordination |
| `repository` | Spring Data persistence interfaces; internal to the owning module |
| `model` | Persisted entities and domain models with private state; never API request/response types |
| `dto.request` | Separate immutable request DTO files with validation |
| `dto.response` | Separate immutable response DTO files |
| `dto` | Internal transfer contracts where request/response is not the appropriate distinction |
| `enums` | Fixed lifecycle/action codes; Java's reserved word `enum` cannot be a package name |
| `exception` | Typed application failures |
| `exception.handler` | Central HTTP/security failure translation and safe error response construction |
| `config` / `config.properties` | Bean wiring and validated configuration properties |
| `constants` | Concern-specific fixed API/technical constants |
| `utils` | Small cohesive technical helpers; no business workflows or mutable global state |
| `mapper` | Explicit DTO/domain/persistence conversion when separation improves cohesion |
| `scheduler`, `filter`, `bootstrap` | Background entry points, request context and explicit initialisation |
| `report` / `report.impl` | Report format strategy contracts and implementations |

Create only packages with actual responsibilities; do not generate empty scaffolding in every module. HTTP controllers remain in the application adapter module; domain modules do not depend on Spring MVC.

Rules:

1. Controllers inject interfaces from `service`, not classes from `service.impl` or repositories.
2. Public service contracts expose DTO/domain-value contracts, not JPA entities or repository APIs.
3. Service implementations access only their own module's repositories/models and other modules' public contracts.
4. JPA entity state is private; access is explicit. Do not solve package migration by making entity fields public.
5. Each request/response DTO has a descriptive standalone name and file, rather than a large nested DTO container.
6. Transaction annotations remain on implementation boundaries; package changes must preserve proxy interception, rollback and locking semantics.
7. Utility, mapping and configuration names describe their responsibility. Avoid wildcard imports and single-letter constructor dependency names.
8. Architecture tests enforce dependency direction, entity encapsulation, DTO placement and implementation hiding. Existing functional tests must still pass after the refactor.
9. Document the concrete module tree and extension points. All code and application documentation stay inside `custody-audit-control`.

Acceptance: no flat production classes remain except the application entry point; callers cannot depend on another module's persistence/implementation; controllers use service interfaces; mapped database tables and externally observable HTTP contracts remain compatible.


## 29. Senior Java Architect and Engineer guardrails

### 29.1 Engineering role and accountability

**Confirmed instruction: Act as a Senior Architect and Java Engineer.** Apply this responsibility to all implementation, refactoring and code review. Deliver readable, well-structured, reusable Java 21 and Spring Boot code that follows the module boundaries and standards in Sections 21–28. Explain significant design decisions and tradeoffs, and preserve business behaviour, authorisation, transaction integrity and audit evidence when changing code.

### 29.2 Descriptive naming conventions

- Use meaningful business and responsibility names for classes, interfaces, methods, fields, parameters, local variables, lambda parameters and test fixtures. Names must communicate intent without requiring the reader to decode shorthand.
- Do not use shortcut identifiers such as `a`, `cb`, `req`, `res`, `obj` or `tmp`. This rule also applies to short lambdas, constructor dependencies and loop variables. Use `auditEvent`, `criteriaBuilder`, `approvalRequest`, `reportResponse`, `changeRequest` or `temporaryReportFile` when those names describe the actual value.
- Use PascalCase for classes and interfaces, lowerCamelCase for methods and variables, UPPER_SNAKE_CASE for constants and enum values, and lowercase responsibility-based package names. Retain the database conventions in Section 21.2.
- Name methods with clear actions, such as `approveChangeRequest` or `findAuthorisedAuditEvents`. Name boolean values and predicates to read clearly, such as `approvalRequired` and `isEligibleForRetentionDeletion`; use plural names for collections.
- Use precise responsibility suffixes where applicable, such as `Service`, `Repository`, `Controller`, `Mapper`, `Request` and `Response`. Avoid vague names such as `Manager`, `Helper`, `CommonUtils` or `BaseService` that hide unrelated responsibilities.
- Established technical terms such as API, DTO, HTTP, JPA and ID may remain where their meaning is clear and consistent. Framework-mandated signatures and external contracts must remain compatible; they do not justify shorthand in application-owned identifiers.

### 29.3 Structured and reusable implementation

- Keep each class cohesive and each method focused on one clear operation. Extract named operations when a method mixes responsibilities or obscures the workflow. Prefer clear control flow and guard clauses over deeply nested conditions or dense stream chains with side effects.
- Follow the package and service boundaries in Section 28. Controllers delegate to service contracts; implementations coordinate use cases; repositories handle persistence; mappers handle conversions. Keep business rules out of controllers, generic utilities and configuration classes.
- Reuse existing audit, approval, permission, report, validation, error and correlation capabilities before adding new infrastructure. Extract shared behaviour only when its meaning and contract are genuinely shared; feature-specific rules remain with the owning module.
- Prefer composition, small explicit contracts and typed inputs over copied workflows, large inheritance hierarchies, boolean-driven multipurpose methods or generic maps. Do not create speculative abstractions or empty layers solely for reuse.
- Use constructor injection, private state and immutable values where practical. Keep dependencies explicit and avoid mutable global state, hidden service lookup and circular module dependencies.
- Maintain consistent formatting and imports. Remove dead code, commented-out implementations and duplicate logic. Comments explain business intent, constraints and tradeoffs that are not evident from the code.
- Refactoring must preserve public contracts, database mappings, permission checks, maker/checker separation, rollback behaviour and audit completeness. Use explicit compatibility decisions and migrations when an intentional contract change is required.

### 29.4 Review and acceptance guardrails

| ID | Required acceptance evidence |
|---|---|
| ENG-01 | Code review confirms descriptive names throughout production code and tests, including lambdas and constructor parameters; shortcut identifiers such as `a` and `cb` are removed. |
| ENG-02 | Classes and methods have clear responsibilities and comply with the package, service and dependency rules in Section 28. Architecture checks enforce those boundaries. |
| ENG-03 | Shared workflows use existing capability contracts; any new abstraction has a documented concrete reuse or extension need and does not duplicate existing infrastructure. |
| ENG-04 | Formatting and configured static-analysis checks pass. Review also checks naming meaning, cohesion and reuse, which automated checks alone cannot establish. |
| ENG-05 | Relevant positive, negative, boundary and regression tests from Section 23 pass, including transaction, authorisation and audit scenarios affected by a change. |
| ENG-06 | The delivery describes the resulting behaviour, significant design decisions, verification performed and remaining limitations. Unimplemented checks or standards must not be reported as completed. |

These guardrails are mandatory implementation and review criteria. Adding them to this PRD establishes requirements; it does not certify that the current code already satisfies them.


## 30. Reusable audit capture architecture

### 30.1 Capture mechanism decision

| Mechanism | Responsibility and decision |
|---|---|
| HTTP filter / interceptor | Establish server-generated correlation context and request diagnostics. It must not copy request bodies into evidence or infer committed changes from HTTP success. |
| Explicit common service method | Selected authoritative capture mechanism: business services call the shared `AuditWriter.append` port with an immutable `AuditDraft`, constructed using descriptive builder methods. Capture happens within the business transaction. |
| Method annotation / AOP | Optional future adapter for narrowly defined activity events; not the authoritative change detector. It would require explicit snapshot and actor contracts and tests for proxy invocation. No generic reflection over arbitrary method arguments or entities. |
| JPA entity listeners / revision history | May complement technical revision tracking; do not replace maker/checker, reason, reference or non-entity import/export evidence. Not added in this implementation. |
| Asynchronous event delivery | Future integration uses a transactional outbox, durable retry and consumer deduplication. Do not move the only evidence write to an in-memory after-commit listener. |

Spring proxy advice is bypassed by self-invocation; transaction-bound listeners run at their configured transaction phase, with AFTER_COMMIT as the default. These framework characteristics inform the explicit transactional capture decision. References: [Spring proxy semantics](https://docs.spring.io/spring-framework/reference/6.2/core/aop/proxying.html) and [transactional event listener contract](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/event/TransactionalEventListener.html).

### 30.2 Contracts, patterns and responsibilities

- **Port and adapter / dependency inversion:** `cc-core.service.AuditWriter` is the shared write contract. `cc-audit.service.impl.AuditWriterImpl` owns persistence, server time, event IDs and correlation fallback. Business modules never access the audit repository.
- **Builder:** descriptive methods name subject, reference, action, status, actor, maker, checker, reason, source and snapshots. Category is an enum. Immutable drafts defensively copy snapshots and centrally reject missing required evidence. Action/status remain extensible business codes because modules have different lifecycles.
- **Interface segregation:** audit query services expose search/detail operations; consumers needing only writing inject only `AuditWriter`. Reporting injects separate read and write contracts.
- **Existing strategy reuse:** feature definitions remain responsible for validating and selecting domain values; report renderers remain format strategies. Do not add an inheritance hierarchy or duplicate recorder facade merely to name another design pattern.
- **Single responsibility:** frontend submits intent; authenticated adapters establish the actor; owning services enforce access and approval and obtain persisted before-values. The writer persists evidence and does not decide authorisation or approval rules.
- **Validation:** malformed internal audit evidence fails the whole business transaction with a stable safe error code. Audit failures are never swallowed. Empty snapshots are legitimate for activities without a record change; missing snapshots, mandatory identity/reference fields, and invalid category are not.
- **Correlation:** preserve the request or worker trace ID; generate a SecureRandom-based ID for direct/background calls lacking context. Never use a shared literal such as “system” as the fallback trace ID.

### 30.3 Transaction and extension rules

A change and its evidence commit atomically. The writer requires an existing transaction; a direct call outside one must fail. Pending, approved and rejected events remain separate facts. Proposed after-values on a pending event do not assert that approved data changed. Failed or rejected HTTP attempts remain diagnostic logs unless a separately specified security-event workflow records them; do not label them committed business changes.

To add a feature: register its validated field definition and scoped permissions, use the existing maker/checker workflow where applicable, then call the shared writer at any additional business transition using a named draft. Supply server-observed snapshots and trusted actor context. Add positive/negative transaction and permission tests. Existing workflow events need no additional per-controller auditing, avoiding duplicates. A frontend-only unsaved edit is not a persisted business change and is not captured as one.

### 30.4 Acceptance criteria

1. Existing workflow, administration, import, report and schedule writers use descriptive draft builders and the shared write port.
2. Search and write implementations are separate; architecture tests prohibit business access to audit persistence and coupling to its implementation.
3. Incomplete evidence is rejected centrally with stable custom error `AUD-500-001` and a safe user message; mutable input maps cannot mutate a built event.
4. Tests verify mandatory transactions, business rollback on audit failure, consistent trace IDs and unique fallback IDs, plus the existing maker/checker and permission regressions.
5. No schema or public HTTP contract change is necessary for this refactor. Future outbox, generic AOP capture and database revision history remain explicitly unimplemented extension options.


## 31. Readable identifiers and method intent

Production Java and test fixtures must use descriptive variable, parameter and lambda names. Replace shorthand such as `e`, `cb`, `r`, `q`, `f`, `req` and `res` with context-specific names such as `auditEvent`, `criteriaBuilder`, `auditEventRoot`, `criteriaQuery`, `searchRequest`, `httpRequest` and `httpResponse`. This applies to exception handlers, schedulers, mappers and configuration callbacks as well as business services. Conventional generic type parameters are not variable names.

Method names and focused documentation must make non-obvious responsibilities clear: building authorised audit criteria, escaping search patterns, and removing restricted fields from responses. Preserve external API paths, JSON names and database contracts during identifier-only refactoring. Validate the changes using compilation, formatting, existing behavioural tests and an automated source-level naming regression check. Automated checks reject prohibited shorthand; review must still assess whether longer names describe their actual meaning.


## 32. Environment profiles and database transition

H2 is a development/test database only. MySQL is the planned production database. Use lowercase Spring environment names `dev`, `sit`, `uat`, `prod`; select exactly one. The default is `dev`. Optional `demo` seeding is permitted only with dev and continues to imply dev for backward compatibility.

Keep shared behaviour in application.properties, H2 and local credentials in application-dev.properties, and environment settings in their respective files. SIT/UAT/prod import one shared MySQL connection configuration. Credentials and JDBC URLs are supplied externally without development fallbacks. Secure cookies are enabled for these HTTPS environments. JPA validates schema and never creates/updates a deployed schema automatically.

Current delivery prepares MySQL connection profiles and includes its JDBC driver; it does not certify MySQL deployment. H2 migrations remain vendor-specific. MySQL schema migrations, accounts/grants and integration testing must be delivered before SIT/UAT/prod rollout; these profiles expect an externally provisioned schema and disable application Flyway migration for now. Future migrations must preserve append-only audit permissions with distinct application, migration and retention identities, foreign keys, index limits, UTC timestamps and large evidence fields.

Retention must use bounded selection and conditional deletion supported by both database vendors; application-level portability is not a substitute for MySQL integration tests. The architecture guide must show the profile/configuration boundary and the same service/repository boundaries for both databases.

Build with JDK 21 using the root Maven reactor. Set the IDE project SDK and Maven runner/importer to Java 21. Verify clean compilation, profile resolution, rejection of conflicting environments/demo in deployed profiles, existing business behaviour and local startup. Record an unreproduced IDE error honestly rather than asserting it has been fixed without its diagnostic.


## 33. Pluggable feature audit adapters and startup identity

Add a classpath banner.txt identifying Custody Audit Control, its audit/maker-checker purpose and Spring Boot version. Do not display credentials or secrets.

Provide `AuditCapture.record(event)` as a shared typed event entry point. Feature modules implement `AuditEventAdapter<T>` with an explicit event class and conversion to a validated AuditDraft. Spring discovers adapter beans; the capture implementation builds an immutable registry once at startup. Duplicate class registrations fail startup, and unknown/null events fail explicitly instead of silently dropping evidence. Dispatch uses exact event classes to avoid inheritance ambiguity. No reflection over arbitrary entity fields or HTTP payloads is permitted.

Apply the adapter pattern to the existing setup workflow so all six features reuse one SetupAuditEventAdapter. Events carry immutable DTO snapshots, authenticated actor and source; the adapter supplies category, reference and maker/checker semantics. Pending, approved and rejected evidence retains its existing meaning and snapshots. Future features using the same setup workflow need no additional adapter. A separate business capability adds its own event DTO and adapter bean without changing the central registry implementation.

AuditCapture requires the caller's transaction and delegates to the existing AuditWriter port. Adapter or persistence failure rolls back the business change. Direct AuditWriter calls remain supported for the existing administration/import/report integrations and must not be combined with capture for the same event. This increment extends section 30 with a concrete reuse need; it does not install generic AOP interception, an event bus or an asynchronous listener.

Organise contracts under core.service and core.audit.spi, capture implementation under audit.service.impl, feature events under workflow.audit.event and adapters under workflow.audit.adapter. Document the dependency direction, extension steps and a concrete code example in docs/audit-integration.md. Verify registry dispatch, duplicate/unknown/null failures, transaction enforcement, existing workflow snapshots and rollback behaviour. SOLID is implemented through small contracts, feature-owned mapping, dependency inversion and extension by bean registration.


## 34. HTTP, authentication, failure and suspicious-activity evidence

### Requirements

| ID | Requirement |
|---|---|
| SEC-01 | Record successful and failed authentication with backend time, outcome, endpoint, direct peer address, bounded user agent, correlation ID and authenticated or attempted subject. Never record credentials. |
| SEC-02 | Record logout, missing authentication, access denial and CSRF denial. |
| SEC-03 | Record one endpoint event for every handled API request, including reads, successful mutations, validation failures, domain errors and unexpected failures. |
| SEC-04 | Use the normalised route template where available rather than exposing resource identifiers as the endpoint name. A separately protected subject/reference may contain a required business identifier. |
| SEC-05 | Persist failed-request evidence in an independent transaction that survives rollback. Preserve same-transaction, fail-closed evidence for successful business mutations. |
| SEC-06 | Classify repeated or policy-defined signals as suspicious with a risk level and explainable rule. Do not claim general fraud/anomaly detection from a small local rule set. |
| SEC-07 | Apply the audit access matrix to security events and exports. Security evidence uses the administration/SYSTEM scope in the initial implementation. |
| SEC-08 | If boundary evidence cannot be persisted after a response is committed, emit a high-severity operational failure containing the correlation ID; monitor and alert on this condition. |
| SEC-09 | Do not persist passwords, password hashes, session IDs, cookies, CSRF tokens, authorisation headers, secrets, unrestricted bodies, stack traces or raw database errors. |
| SEC-10 | Version the event schema and retain renderability of older rows. |

A completed business request normally produces both a domain event and an HTTP endpoint event. These describe different facts and are not duplicates. Reading the audit API is itself audited after the response snapshot is selected; this does not recursively invoke the HTTP stack.

### Audit event table dictionary

| Column | Meaning and rules |
|---|---|
| `id` | Unpredictable immutable event identifier and primary key. |
| `schema_version` | Version of the stored event contract. Existing migrated rows remain version 1; newly written rows use version 2. |
| `event_type` | Stable machine event such as BUSINESS_CHANGE, API_REQUEST, AUTHENTICATION_SUCCESS, AUTHENTICATION_FAILURE, LOGOUT, ACCESS_DENIED, VALIDATION_FAILURE, APPLICATION_ERROR or SUSPICIOUS_ACTIVITY. |
| `category` | STATIC, BUSINESS, ADMIN or SECURITY report grouping. |
| `feature`, `scope` | Durable authorisation pair evaluated before search, count, detail and export. |
| `record_id` | Stable business subject, attempted login subject, or normalised endpoint when no business subject exists. |
| `reference_id` | Change-request, batch, job or request correlation reference joining related evidence. |
| `action`, `status` | Domain action and status-at-event; never overwritten with a later state. |
| `outcome` | Explicit SUCCESS or FAILURE for HTTP/security activity. Domain events continue to use their status semantics. |
| `actor` | Authenticated user/service identity; ANONYMOUS when authentication did not succeed. |
| `maker`, `checker` | Maker/checker identity snapshots where relevant; security activity uses actor as maker and an empty checker. |
| `reason` | Safe business reason or explainable classification, never exception internals. |
| `source` | Originating channel, such as Setup screen, Import, Scheduler or HTTP. |
| `trace_id` | Correlation ID shared with the response and operational logs. |
| `occurred_at` | Immutable backend UTC timestamp used for ordering and retention. |
| `before_values`, `proposed_values`, `after_values` | Allowlisted snapshots. Pending/rejected after-values remain equal to before-values. |
| `endpoint` | Normalised route template where MVC supplies it; security-filter endpoints use the central configured path. Query strings are excluded. |
| `http_method`, `http_status` | HTTP verb and final response status. Nullable for non-HTTP domain evidence. |
| `error_code` | Safe custom feature code such as `WF-409-003`; no exception message or SQL detail. |
| `source_ip` | Direct network peer only. Forwarded headers are not trusted without an explicitly configured trusted-proxy boundary. |
| `user_agent` | Sanitised and length-bounded client string. It is untrusted evidence, not an identity. |
| `risk_level` | LOW, MEDIUM or HIGH classification produced by an explainable rule. |
| `duration_ms` | Bounded request processing duration for operational investigation, not billing. |
| `metadata` | Small versioned allowlisted key/value context. It must never become a request-body dumping field. |

### Classification and corner cases

| Situation | Stored event/outcome | Transaction behavior |
|---|---|---|
| Valid login | AUTHENTICATION_SUCCESS / SUCCESS | Independent committed evidence |
| Bad username, password or inactive account | AUTHENTICATION_FAILURE / FAILURE; attempted username is subject, actor is ANONYMOUS | Independent committed evidence |
| Repeated failed login threshold reached | SUSPICIOUS_ACTIVITY / FAILURE / HIGH | Same independent path; original failures remain |
| Logout | LOGOUT / SUCCESS | Independent committed evidence |
| Missing session | `IDN-401-001` / FAILURE | Independent committed evidence |
| Permission or CSRF denial | ACCESS_DENIED / FAILURE / MEDIUM | Independent committed evidence |
| DTO/type/body validation | VALIDATION_FAILURE / FAILURE with safe code | Survives rejected request |
| Domain conflict or stale version | Failed endpoint event plus unchanged prior domain evidence | Failed business transaction does not emit a successful domain event |
| Unexpected application/database failure | APPLICATION_ERROR / FAILURE / HIGH plus protected operational stack trace | Best possible independent evidence; capture failure itself is alerted |
| Successful read | API_REQUEST / SUCCESS | Independent evidence |
| Successful mutation | API_REQUEST plus domain-specific event | Domain event is atomic with mutation; endpoint event is independent |
| Process kill, database outage or failure before application control | Application evidence may be impossible | Infrastructure/access/database logs and monitoring are mandatory complementary controls |

The initial suspicious rule is five failed logins for one direct-IP/attempted-username pair in ten minutes, with configurable threshold/window and bounded local memory. It is not cluster-wide. Production must forward these immutable events to a SIEM or replace the detector with a distributed strategy for cross-node correlation, credential stuffing, impossible travel, enumeration, high-rate access denial and other approved use cases.

### Central API mapping

All backend paths, including parameterised child endpoints, are declared in `com.custody.app.constants.ApiRoutes`. Controllers, Spring Security and interceptors reference that catalogue. `CC_CONTEXT_PATH` relocates the whole deployment at runtime, and the browser derives its API root from `document.baseURI`. The versioned `/api/v1` portion remains an intentional API contract: changing it requires coordinated backend/client compatibility testing rather than an unsafe runtime mismatch.


## 35. Typed exception implementation and global translation

Each feature owns an `ErrorCatalog` containing immutable `ErrorDefinition` values. Every definition independently declares a stable public code, HTTP status, safe message key, exception category and retry policy. Codes use feature prefixes and are stored unchanged in audit evidence. New feature failures do not require a central enum change or one empty exception class per code.

The sealed `AppException` hierarchy is deliberately bounded to validation, authentication, authorisation, not-found, conflict, business-rule, infrastructure and internal categories. `AppException.of` and `require` accept any feature definition and select the category exception. Spring discovers catalogue beans; `ErrorCatalogRegistry` fails startup when public codes collide or safe messages are missing. Architecture tests additionally validate code/message uniqueness and category coverage.

`GlobalExceptionHandler` translates the common application base type and adapts MVC/framework failures. It must preserve the intended 400, 401, 403, 404, 405, 409, 410, 415, 422, 500 and 503 semantics. Recognised unique constraint signatures may map to the configured conflict definition such as `SYS-409-001`; unknown integrity failures must remain internal failures. Expected application/client failures are logged without repetitive stack traces. Unexpected, infrastructure and unclassified persistence failures retain diagnostic causes only in protected logs.

Every handled response uses problem JSON with the definition's status and custom code, its centrally resolved safe message, server trace ID and immutable field errors, plus no-store caching. `CorrelationFilter` generates the trace from 16 `SecureRandom` bytes, returns it in `X-Correlation-ID`, and scopes it in MDC. The response factory also places the safe code on the request so the independent failed-request audit event records the same error code and trace ID. Caller correlation values and exception messages are not trusted as evidence. Audit outcome is the separate generic `SUCCESS` or `FAILURE`; it never replaces the exact feature error code.

Authentication/security failures rejected before MVC use the same response factory in their boundary handlers. Background jobs and streaming failures remain explicit responsibilities of the owning worker because controller advice cannot intercept them. Failed HTTP attempt evidence commits in its independent transaction; successful business mutation evidence remains atomic with the business transaction. The detailed architecture, mapping table and extension checklist are maintained in `docs/exception-architecture.md`.
