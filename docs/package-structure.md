# Package structure and dependency rules

The PRD v1.8 package standard is implemented in the source tree. Packages are created where a module has that responsibility; domain modules do not contain empty HTTP layers.

## Module packages

### cc-application

| Package | Types |
|---|---|
| `com.custody.app` | `CustodyApplication` |
| `com.custody.app.bootstrap` | `DemoBootstrap` |
| `com.custody.app.audit` | `ApiAuditInterceptor`, `ApiAuditPublisher`, `AuditRequestAttributes`, `AuditRiskAssessment`, `SuspiciousActivityDetector` |
| `com.custody.app.config` | `EnvironmentProfileValidator`, `SecurityConfiguration`, `WebConfiguration` |
| `com.custody.app.config.properties` | `RetentionProperties`, `SuspiciousActivityProperties` |
| `com.custody.app.constants` | `ApiRoutes` |
| `com.custody.app.controller` | `AdministrationController`, `AuditController`, `ImportController`, `ReportController`, `SessionController`, `WorkflowController` |
| `com.custody.app.dto.response` | `ErrorResponse` |
| `com.custody.app.error` | `SystemErrorCatalog`, `ErrorCatalogRegistry` |
| `com.custody.app.exception.handler` | `ErrorResponseFactory`, `GlobalExceptionHandler` |
| `com.custody.app.filter` | `CorrelationFilter` |
| `com.custody.app.scheduler` | `JobRunner`, `RetentionJob` |

### cc-audit

| Package | Types |
|---|---|
| `com.custody.audit.dto.request` | `AuditSearchRequest` |
| `com.custody.audit.dto.response` | `AuditEventResponse`, `AuditPageResponse` |
| `com.custody.audit.error` | `AuditErrorCatalog` |
| `com.custody.audit.model` | `AuditEvent` |
| `com.custody.audit.repository` | `AuditRepository` |
| `com.custody.audit.service` | `AuditService` |
| `com.custody.audit.service.impl` | `AuditActivityRecorderImpl`, `AuditCaptureImpl`, `AuditServiceImpl`, `AuditWriterImpl` |

### cc-core

| Package | Types |
|---|---|
| `com.custody.core.builder` | `AuditActivityDraftBuilder`, `AuditDraftBuilder` |
| `com.custody.core.dto` | `AuditActivityDraft`, `AuditDraft` |
| `com.custody.core.enums` | `AuditCategory`, `AuditOutcome`, `Action`, `ChangeAction`, `ErrorCategory`, `OperationalStatus`, `RequestStatus` |
| `com.custody.core.error` | `ErrorDefinition`, `ErrorCatalog`, `CoreErrorCatalog` |
| `com.custody.core.exception` | Sealed `AppException` and eight final handling-category exceptions; see `exception-architecture.md` |
| `com.custody.core.service` | `AuditActivityRecorder`, `AuditCapture`, `AuditWriter` |
| `com.custody.core.utils` | `Ids`, `JsonValues` |

### cc-identity

| Package | Types |
|---|---|
| `com.custody.identity.dto` | `AuthenticationCredential`, `ScopedGrant` |
| `com.custody.identity.dto.request` | `SecurityChangeRequest` |
| `com.custody.identity.dto.response` | `DirectoryResponse`, `GrantResponse`, `GroupResponse`, `SecurityChangeResponse`, `SessionResponse`, `UserResponse` |
| `com.custody.identity.error` | `IdentityErrorCatalog` |
| `com.custody.identity.mapper` | `IdentityResponseMapper` |
| `com.custody.identity.model` | `Membership`, `PermissionGrant`, `SecurityChange`, `UserAccount`, `UserGroup` |
| `com.custody.identity.repository` | `MembershipRepository`, `PermissionGrantRepository`, `SecurityChangeRepository`, `UserAccountRepository`, `UserGroupRepository` |
| `com.custody.identity.service` | `AccessPolicy`, `IdentityService` |
| `com.custody.identity.service.impl` | `AccessPolicyImpl`, `IdentityServiceImpl` |

### cc-reporting

| Package | Types |
|---|---|
| `com.custody.reporting.config.properties` | `ReportProperties` |
| `com.custody.reporting.dto` | `ReportDownload` |
| `com.custody.reporting.dto.request` | `CreateScheduleRequest`, `GenerateReportRequest` |
| `com.custody.reporting.dto.response` | `ReportResponse`, `ScheduleResponse` |
| `com.custody.reporting.enums` | `ReportFormat`, `ReportStatus` |
| `com.custody.reporting.error` | `ReportingErrorCatalog` |
| `com.custody.reporting.mapper` | `ReportResponseMapper` |
| `com.custody.reporting.model` | `ReportArtifact`, `ReportJob`, `ReportSchedule` |
| `com.custody.reporting.report` | `ReportRenderer` |
| `com.custody.reporting.report.impl` | `CsvRenderer`, `ExcelRenderer` |
| `com.custody.reporting.repository` | `ReportArtifactRepository`, `ReportJobRepository`, `ReportScheduleRepository` |
| `com.custody.reporting.service` | `ReportScheduleService`, `ReportService`, `ReportWorkerService` |
| `com.custody.reporting.service.impl` | `ReportServiceImpl` |
| `com.custody.reporting.utils` | `ReportColumns` |

### cc-workflow

| Package | Types |
|---|---|
| `com.custody.workflow.config` | `FeatureConfiguration` |
| `com.custody.workflow.dto` | `ImportResult` |
| `com.custody.workflow.dto.request` | `ApprovalDecisionRequest`, `ChangeSetupStatusRequest`, `CreateSetupRequest`, `UpdateSetupRequest` |
| `com.custody.workflow.dto.response` | `ChangeRequestResponse`, `SetupRecordResponse` |
| `com.custody.workflow.mapper` | `WorkflowResponseMapper` |
| `com.custody.workflow.error` | `WorkflowErrorCatalog` |
| `com.custody.workflow.model` | `ChangeRequest`, `FeatureDefinition`, `SetupRecord` |
| `com.custody.workflow.repository` | `ChangeRequestRepository`, `SetupRecordRepository` |
| `com.custody.workflow.service` | `FeatureRegistry`, `ImportService`, `WorkflowService` |
| `com.custody.workflow.service.impl` | `FeatureRegistryImpl`, `ImportServiceImpl`, `WorkflowServiceImpl` |

## Example use-case flow

`WorkflowController` validates a request DTO and calls `WorkflowService`. `WorkflowServiceImpl` coordinates authorisation, the transaction, owned repositories and the `AuditWriter` contract. `WorkflowResponseMapper` converts internal entities to standalone response DTOs. Controllers never receive a repository or implementation dependency.

Report access, scheduling and worker execution expose separate interfaces (`ReportService`, `ReportScheduleService`, `ReportWorkerService`). One transactional implementation currently implements those contracts; clients inject only the contract they need. Format strategies remain in `report` / `report.impl`.

## Enforced rules

- Entities live in `model` and all persistent fields are private. Database names and mappings are unchanged.
- Repository interfaces live in `repository` and cannot be used across module boundaries.
- Contracts in `service` are interfaces and do not expose entities.
- Implementations live in `service.impl`; external modules cannot reference them.
- Request and response DTOs are standalone records in `dto.request` and `dto.response`.
- Fixed enums live in `enums`; dynamic groups and permissions remain persisted data.
- Error translation lives in `exception.handler`; safe error DTOs and message resources remain central.
- Constructor injection uses descriptive dependency names. Explicit imports and formatting are standard.
- The ArchUnit suite enforces these rules alongside the existing integration scenarios.

Later PRD increments intentionally add typed capture, three-state snapshots and HTTP/security evidence; see the architecture and integration guides for those contracts.


Additional audit extension packages: `com.custody.core.audit.spi` contains `AuditEventAdapter`; `com.custody.workflow.audit.event` contains `SetupAuditEvent`; `com.custody.workflow.audit.adapter` contains `SetupAuditEventAdapter`. These packages isolate feature event mapping from shared persistence.
