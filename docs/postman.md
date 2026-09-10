# Complete Postman API collection

There is one canonical, import-ready Postman collection and one matching environment:

- `Custody-Audit-Control.postman_collection.json`
- `Custody-Audit-Control.local.postman_environment.json`

The collection is generated from `scripts/generate-postman.js`. Regenerate it after an API-contract change with:

```sh
node scripts/generate-postman.js
```

## Run the collection

Start the demo application on port 8091, import both JSON files, select **Custody Audit Control — Local Complete API**, and keep Postman's cookie jar enabled. Spring Security stores authentication and CSRF state in the session.

```sh
./scripts/run.sh --server.port=8091
```

Run folders 01 through 09 in order. In folder 09, deselect **Internal error contract — saved example only, do not run**. That item holds a response example; the application intentionally provides no endpoint that crashes on demand.

The collection switches between `maker`, `checker` and `reviewer`, automatically stores returned IDs and versions, and demonstrates the full maker-checker lifecycle. The report-list request polls for up to ten seconds until the asynchronous report becomes ready for download. For the multipart import, Postman may require you to reselect `docs/vault-import.csv` because imported collections cannot grant access to local files.

## Endpoint coverage

| Controller/boundary | Method and endpoint | Input represented in Postman |
|---|---|---|
| Security | `POST /api/v1/login` | URL-encoded username/password, success and failure |
| Security | `POST /api/v1/logout` | CSRF header, no body |
| Session | `GET /api/v1/csrf` | No input |
| Session | `GET /api/v1/session` | No input |
| Session | `GET /api/v1/features` | No input |
| Workflow | `GET /api/v1/features/{feature}/records` | `feature` path variable |
| Workflow | `POST /api/v1/features/{feature}/records` | `feature` path variable and complete create body |
| Workflow | `POST /api/v1/records/{id}/edit` | `id` path variable, version, values and reason |
| Workflow | `POST /api/v1/records/{id}/status` | `id` path variable, version, status and reason |
| Workflow | `GET /api/v1/changes` | No input |
| Workflow | `POST /api/v1/changes/{id}/decision` | `id` path variable, approval flag and reason |
| Administration | `GET /api/v1/administration` | No input |
| Administration | `POST /api/v1/administration/changes` | Complete security-change body |
| Administration | `POST /api/v1/administration/changes/{id}/decision` | `id` path variable and decision body |
| Import | `POST /api/v1/features/{feature}/import` | `feature` path variable, `scope` query parameter and multipart file |
| Audit | `GET /api/v1/audit` | All 12 optional filters plus `page` and `size` query parameters |
| Audit | `GET /api/v1/audit/{id}` | `id` path variable |
| Reporting | `GET /api/v1/reports` | No input |
| Reporting | `POST /api/v1/reports` | Format and complete audit filter body |
| Reporting | `GET /api/v1/reports/{id}/download` | `id` path variable |
| Reporting | `GET /api/v1/schedules` | No input |
| Reporting | `POST /api/v1/schedules` | Complete schedule body |
| Reporting | `POST /api/v1/schedules/{id}/toggle` | `id` path variable, no body |

Every request has a description, parameter descriptions, assertions, and at least one saved response example. The error section includes malformed JSON, DTO field validation, feature validation, audit validation, missing resource, unknown endpoint, unsupported method, unsupported media type, forbidden access, invalid credentials, unauthenticated access and the safe internal-error contract.

## Trace-ID lifecycle

`CorrelationFilter` generates a cryptographically random 128-bit identifier using `Ids.next()` for every HTTP request. It ignores any client-supplied `X-Correlation-ID`, writes the trusted value to the response header and keeps it in SLF4J MDC for the duration of request processing.

The same value is stored as `cc_audit_event.trace_id`. For HTTP/security evidence it is also the `reference_id`. Error responses return it as `traceId`, allowing one identifier to join the client response, immutable audit evidence and structured application logs.

Every live collection request checks that `X-Correlation-ID` is 32 lowercase hexadecimal characters, saves it as `lastTraceId`, and prints it to the Postman Console.

Example safe validation response:

```json
{
  "status": 400,
  "code": "SYS-400-001",
  "message": "Some input is invalid. Check the fields and date range, then try again.",
  "traceId": "7d176f7bf89ac6ab70f9ec0352bc2f71",
  "fieldErrors": {
    "businessKey": "must not be blank"
  }
}
```

Clients should branch on stable `code`, not English text. Exception types, SQL errors, stack traces, passwords, cookies, tokens and unrestricted request/response bodies are never returned or stored in audit evidence.

## Transaction behavior

- A successful business change and its domain audit event share one transaction. Either both commit or both roll back.
- Authentication, endpoint and failed-request evidence uses `REQUIRES_NEW`, allowing the failure event to commit after the rejected business transaction rolls back.
- If the audit database itself is unavailable, persistence is impossible. Boundary capture logs `audit_activity_capture_failure` with correlation context for operational alerting.
