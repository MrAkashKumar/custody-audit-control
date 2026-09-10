const fs = require("node:fs");
const path = require("node:path");

const docs = path.resolve(__dirname, "../docs");
const collectionFile = path.join(docs, "Custody-Audit-Control.postman_collection.json");
const environmentFile = path.join(docs, "Custody-Audit-Control.local.postman_environment.json");

const jsonHeader = [{ key: "Content-Type", value: "application/json", type: "text" }];
const csrfHeader = [{ key: "{{csrfHeader}}", value: "{{csrfToken}}", type: "text" }];
const csrfJsonHeaders = [...csrfHeader, ...jsonHeader];

const sampleTrace = "42f7c2ee692afe818fa42f3a7715c220";
const problem = (status, code, message, fieldErrors = {}) => ({
  status,
  code,
  message,
  traceId: sampleTrace,
  fieldErrors,
});

function postmanUrl(pathTemplate, pathVariables = [], query = []) {
  const enabled = query.filter((entry) => !entry.disabled);
  const suffix = enabled.length
    ? `?${enabled.map((entry) => `${entry.key}=${entry.value}`).join("&")}`
    : "";
  return {
    raw: `{{baseUrl}}${pathTemplate}${suffix}`,
    host: ["{{baseUrl}}"],
    path: pathTemplate.split("/").filter(Boolean),
    ...(query.length ? { query } : {}),
    ...(pathVariables.length ? { variable: pathVariables } : {}),
  };
}

function savedResponse(name, status, body, originalRequest, contentType = "application/json") {
  const responseBody = typeof body === "string" ? body : JSON.stringify(body, null, 2);
  return {
    name,
    originalRequest: JSON.parse(JSON.stringify(originalRequest)),
    status: status === 200 ? "OK" : status === 202 ? "Accepted" : status === 204 ? "No Content" :
      status === 400 ? "Bad Request" : status === 401 ? "Unauthorized" : status === 403 ? "Forbidden" :
      status === 404 ? "Not Found" : status === 405 ? "Method Not Allowed" :
      status === 409 ? "Conflict" : status === 415 ? "Unsupported Media Type" : "Internal Server Error",
    code: status,
    _postman_previewlanguage: contentType.includes("json") ? "json" : "text",
    header: status === 204 ? [{ key: "X-Correlation-ID", value: sampleTrace }] : [
      { key: "Content-Type", value: contentType },
      { key: "X-Correlation-ID", value: sampleTrace },
    ],
    cookie: [],
    body: status === 204 ? "" : responseBody,
  };
}

function apiRequest({
  name,
  method,
  path: requestPath,
  description,
  headers = [],
  body,
  pathVariables = [],
  query = [],
  status = 200,
  tests = [],
  example,
  exampleName,
  exampleContentType,
  prerequest = [],
}) {
  const request = {
    method,
    header: headers,
    ...(body ? { body } : {}),
    url: postmanUrl(requestPath, pathVariables, query),
    description,
  };
  const events = [];
  if (prerequest.length) {
    events.push({ listen: "prerequest", script: { type: "text/javascript", exec: prerequest } });
  }
  events.push({
    listen: "test",
    script: {
      type: "text/javascript",
      exec: [`pm.test("HTTP ${status}", () => pm.response.to.have.status(${status}));`, ...tests],
    },
  });
  return {
    name,
    request,
    event: events,
    response: [savedResponse(exampleName || `${status} response`, status, example, request, exampleContentType)],
  };
}

const jsonBody = (value) => ({ mode: "raw", raw: JSON.stringify(value, null, 2), options: { raw: { language: "json" } } });
const rawJsonBody = (value) => ({ mode: "raw", raw: value, options: { raw: { language: "json" } } });
const pathVar = (key, value, description) => ({ key, value, description });
const queryParam = (key, value, description, disabled = false) => ({ key, value, description, disabled });

const changeExample = {
  id: "a1b2c3d4e5f647889900aabbccddeeff",
  feature: "vault",
  scope: "BULLION",
  businessKey: "POSTMAN-EXAMPLE",
  recordId: null,
  action: "ADD",
  status: "PENDING",
  before: {},
  proposed: { name: "Postman Enterprise Vault", location: "Singapore", operator: "Custody Operations" },
  maker: "maker",
  checker: null,
  reason: "Postman complete API example",
  decisionReason: null,
  submittedAt: "2026-09-10T06:50:00Z",
  decidedAt: null,
};
const recordExample = {
  id: "b1b2c3d4e5f647889900aabbccddeeff",
  feature: "vault",
  scope: "BULLION",
  businessKey: "POSTMAN-EXAMPLE",
  values: { name: "Postman Enterprise Vault", location: "Singapore", operator: "Custody Operations" },
  status: "ACTIVE",
  version: 0,
  updatedAt: "2026-09-10T06:51:00Z",
};
const auditExample = {
  id: "c1b2c3d4e5f647889900aabbccddeeff",
  schemaVersion: 1,
  eventType: "DOMAIN",
  category: "STATIC",
  feature: "vault",
  scope: "BULLION",
  recordId: recordExample.id,
  reference: changeExample.id,
  action: "ADD",
  status: "APPROVED",
  actor: "checker",
  maker: "maker",
  checker: "checker",
  reason: "Independent approval",
  source: "Approval review",
  traceId: sampleTrace,
  endpoint: "/api/v1/changes/{id}/decision",
  httpMethod: "POST",
  httpStatus: 200,
  outcome: "SUCCESS",
  errorCode: null,
  sourceIp: "127.0.0.1",
  userAgent: "PostmanRuntime",
  riskLevel: "LOW",
  durationMs: 18,
  metadata: {},
  occurredAt: "2026-09-10T06:51:00Z",
  before: {},
  proposed: changeExample.proposed,
  after: { ...changeExample.proposed, operationalStatus: "ACTIVE" },
};
const reportExample = {
  id: "d1b2c3d4e5f647889900aabbccddeeff",
  owner: "maker",
  audienceGroup: null,
  trigger: "MANUAL",
  format: "CSV",
  status: "REQUESTED",
  rowCount: 0,
  requestedAt: "2026-09-10T06:52:00Z",
  expiresAt: "2026-09-17T06:52:00Z",
  failureCode: null,
  traceId: sampleTrace,
};
const scheduleExample = {
  id: "e1b2c3d4e5f647889900aabbccddeeff",
  name: "Postman daily Vault audit",
  owner: "maker",
  audienceGroup: "csa",
  category: "STATIC",
  feature: "vault",
  scope: "BULLION",
  format: "CSV",
  enabled: true,
  intervalDays: 1,
  nextRunAt: "2026-09-11T06:52:00Z",
  lastFailure: null,
};

function login(name, userVariable) {
  return apiRequest({
    name,
    method: "POST",
    path: "/api/v1/login",
    headers: [...csrfHeader, { key: "Content-Type", value: "application/x-www-form-urlencoded", type: "text" }],
    body: {
      mode: "urlencoded",
      urlencoded: [
        { key: "username", value: `{{${userVariable}}}`, description: "Demo user identifier", type: "text" },
        { key: "password", value: "{{demoPassword}}", description: "Secret environment value", type: "text" },
      ],
    },
    description: "Spring Security login endpoint. Maintains the authenticated session in Postman's cookie jar.",
    example: { authenticated: true },
    tests: [`pm.test("Authentication response", () => pm.expect(pm.response.json().authenticated).to.eql(true));`],
  });
}

const collection = {
  info: {
    _postman_id: "2465eaa9-5401-4fc0-a49f-e74c5f4f2f81",
    name: "Custody Audit Control — Complete API, Audit and Error Collection",
    description: "Canonical collection generated from scripts/generate-postman.js. Covers every controller/security endpoint, with headers, request bodies, query parameters, path variables, chaining scripts, tests and saved response examples. Run folders 01-08 in order with the matching environment and cookie jar enabled. Folder 09 is a response-contract catalogue; its 500 item is documentation-only.",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
  },
  variable: [
    { key: "csrfHeader", value: "X-CSRF-TOKEN" },
    { key: "csrfToken", value: "" },
    { key: "businessKey", value: "" },
    { key: "changeRequestId", value: "" },
    { key: "recordId", value: "" },
    { key: "recordVersion", value: "0" },
    { key: "editChangeId", value: "" },
    { key: "statusChangeId", value: "" },
    { key: "adminGroupId", value: "" },
    { key: "adminChangeId", value: "" },
    { key: "auditId", value: "" },
    { key: "reportId", value: "" },
    { key: "scheduleId", value: "" },
    { key: "lastTraceId", value: "" },
  ],
  event: [{
    listen: "test",
    script: { type: "text/javascript", exec: [
      "const traceId = pm.response.headers.get(\"X-Correlation-ID\");",
      "pm.test(\"Trusted correlation ID header\", () => pm.expect(traceId).to.match(/^[0-9a-f]{32}$/));",
      "if (traceId) { pm.collectionVariables.set(\"lastTraceId\", traceId); console.log(\"Audit traceId:\", traceId); }",
    ] },
  }],
  item: [],
};

collection.item.push({ name: "01 — Security and session controller", item: [
  apiRequest({
    name: "Get CSRF token",
    method: "GET",
    path: "/api/v1/csrf",
    description: "Public CSRF bootstrap endpoint. Stores the dynamic header name and token for all state-changing requests.",
    example: { headerName: "X-CSRF-TOKEN", token: "example-token" },
    tests: [
      "const body = pm.response.json();",
      "pm.collectionVariables.set(\"csrfHeader\", body.headerName);",
      "pm.collectionVariables.set(\"csrfToken\", body.token);",
    ],
  }),
  login("Login as maker", "makerUsername"),
  apiRequest({
    name: "Get authenticated session",
    method: "GET",
    path: "/api/v1/session",
    description: "Returns the current user, active group memberships and effective grants.",
    example: { user: "maker", name: "Maya Tan", groups: [{ id: "bullion", name: "Bullion Clearing", active: true }], grants: [{ id: "grant-id", groupId: "bullion", feature: "*", action: "VIEW", scope: "BULLION" }] },
    tests: ["pm.test(\"Maker session\", () => pm.expect(pm.response.json().user).to.eql(pm.environment.get(\"makerUsername\")));"]
  }),
  apiRequest({
    name: "List accessible feature definitions",
    method: "GET",
    path: "/api/v1/features",
    description: "Returns feature metadata and required field definitions visible to the authenticated actor.",
    example: [{ key: "vault", label: "Vault", fields: [{ key: "name", label: "Vault name", type: "text", required: true }, { key: "location", label: "Location", type: "text", required: true }, { key: "operator", label: "Operator", type: "text", required: true }] }],
    tests: ["pm.test(\"Feature array\", () => pm.expect(pm.response.json()).to.be.an(\"array\"));"]
  }),
] });

collection.item.push({ name: "02 — Maker workflow controller", item: [
  apiRequest({
    name: "List records by feature",
    method: "GET",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key, for example vault or holiday-calendar")],
    description: "Path variable: feature. Lists records the actor may view for that feature.",
    example: [recordExample],
    tests: ["pm.test(\"Record array\", () => pm.expect(pm.response.json()).to.be.an(\"array\"));"]
  }),
  apiRequest({
    name: "Create record proposal",
    method: "POST",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    headers: csrfJsonHeaders,
    body: rawJsonBody("{\n  \"businessKey\": \"{{businessKey}}\",\n  \"scope\": \"{{scope}}\",\n  \"values\": {\n    \"name\": \"Postman Enterprise Vault\",\n    \"location\": \"Singapore\",\n    \"operator\": \"Custody Operations\"\n  },\n  \"reason\": \"Postman complete API example\"\n}"),
    prerequest: ["pm.collectionVariables.set(\"businessKey\", `POSTMAN-${Date.now()}`);"],
    description: "Path variable: feature. Full CreateSetupRequest body. Returns 202 because maker input remains pending until independent approval.",
    status: 202,
    example: changeExample,
    tests: [
      "const body = pm.response.json();",
      "pm.collectionVariables.set(\"changeRequestId\", body.id);",
      "pm.test(\"Pending ADD proposal\", () => { pm.expect(body.action).to.eql(\"ADD\"); pm.expect(body.status).to.eql(\"PENDING\"); });",
    ],
  }),
  apiRequest({
    name: "List pending changes",
    method: "GET",
    path: "/api/v1/changes",
    description: "Returns pending requests visible to the actor. No request parameters.",
    example: [changeExample],
    tests: ["pm.test(\"Change array\", () => pm.expect(pm.response.json()).to.be.an(\"array\"));"]
  }),
] });

collection.item.push({ name: "03 — Checker approval and record updates", item: [
  login("Login as checker", "checkerUsername"),
  apiRequest({
    name: "Approve create proposal",
    method: "POST",
    path: "/api/v1/changes/:id/decision",
    pathVariables: [pathVar("id", "{{changeRequestId}}", "Change request ID returned by create/edit/status/import")],
    headers: csrfJsonHeaders,
    body: jsonBody({ approve: true, reason: "Independent checker approval" }),
    description: "Path variable: id. Full ApprovalDecisionRequest body. Maker and checker must be different users.",
    example: { ...changeExample, recordId: recordExample.id, status: "APPROVED", checker: "checker", decisionReason: "Independent checker approval", decidedAt: "2026-09-10T06:51:00Z" },
    tests: [
      "const body = pm.response.json();",
      "pm.collectionVariables.set(\"recordId\", body.recordId);",
      "pm.test(\"Approved\", () => pm.expect(body.status).to.eql(\"APPROVED\"));",
    ],
  }),
  login("Switch back to maker", "makerUsername"),
  apiRequest({
    name: "Refresh records and capture version",
    method: "GET",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    description: "Finds the newly approved business key and stores its record ID and optimistic-lock version.",
    example: [recordExample],
    tests: [
      "const row = pm.response.json().find(item => item.businessKey === pm.collectionVariables.get(\"businessKey\"));",
      "pm.test(\"Approved record exists\", () => pm.expect(row).to.be.an(\"object\"));",
      "if (row) { pm.collectionVariables.set(\"recordId\", row.id); pm.collectionVariables.set(\"recordVersion\", String(row.version)); }",
    ],
  }),
  apiRequest({
    name: "Propose record edit",
    method: "POST",
    path: "/api/v1/records/:id/edit",
    pathVariables: [pathVar("id", "{{recordId}}", "Existing setup record ID")],
    headers: csrfJsonHeaders,
    body: rawJsonBody("{\n  \"version\": {{recordVersion}},\n  \"values\": {\n    \"name\": \"Postman Enterprise Vault Updated\",\n    \"location\": \"Singapore\",\n    \"operator\": \"Custody Operations\"\n  },\n  \"reason\": \"Demonstrate optimistic versioned edit\"\n}"),
    description: "Path variable: id. Full UpdateSetupRequest body; version is the current server version.",
    example: { ...changeExample, id: "edit-change-id", recordId: recordExample.id, action: "EDIT", before: recordExample.values, proposed: { ...recordExample.values, name: "Postman Enterprise Vault Updated" } },
    tests: ["pm.collectionVariables.set(\"editChangeId\", pm.response.json().id);"],
  }),
  login("Login as checker for edit approval", "checkerUsername"),
  apiRequest({
    name: "Approve edit proposal",
    method: "POST",
    path: "/api/v1/changes/:id/decision",
    pathVariables: [pathVar("id", "{{editChangeId}}", "Edit change request ID")],
    headers: csrfJsonHeaders,
    body: jsonBody({ approve: true, reason: "Edit checked independently" }),
    description: "Approves the edit and atomically records its domain audit evidence.",
    example: { ...changeExample, id: "edit-change-id", recordId: recordExample.id, action: "EDIT", status: "APPROVED", checker: "checker" },
  }),
  login("Login as maker for status change", "makerUsername"),
  apiRequest({
    name: "Refresh version after edit",
    method: "GET",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    description: "Refreshes the optimistic version before the status proposal.",
    example: [{ ...recordExample, version: 1, values: { ...recordExample.values, name: "Postman Enterprise Vault Updated" } }],
    tests: [
      "const row = pm.response.json().find(item => item.id === pm.collectionVariables.get(\"recordId\"));",
      "pm.test(\"Record available\", () => pm.expect(row).to.be.an(\"object\"));",
      "if (row) pm.collectionVariables.set(\"recordVersion\", String(row.version));",
    ],
  }),
  apiRequest({
    name: "Propose record status change",
    method: "POST",
    path: "/api/v1/records/:id/status",
    pathVariables: [pathVar("id", "{{recordId}}", "Existing setup record ID")],
    headers: csrfJsonHeaders,
    body: rawJsonBody("{\n  \"version\": {{recordVersion}},\n  \"status\": \"INACTIVE\",\n  \"reason\": \"Controlled operational status change\"\n}"),
    description: "Path variable: id. Full ChangeSetupStatusRequest body. Allowed status values: ACTIVE, INACTIVE.",
    example: { ...changeExample, id: "status-change-id", recordId: recordExample.id, action: "DEACTIVATE" },
    tests: ["pm.collectionVariables.set(\"statusChangeId\", pm.response.json().id);"],
  }),
  login("Login as checker for status approval", "checkerUsername"),
  apiRequest({
    name: "Approve status proposal",
    method: "POST",
    path: "/api/v1/changes/:id/decision",
    pathVariables: [pathVar("id", "{{statusChangeId}}", "Status change request ID")],
    headers: csrfJsonHeaders,
    body: jsonBody({ approve: true, reason: "Status change checked independently" }),
    description: "Applies the approved status transition and captures checker evidence.",
    example: { ...changeExample, id: "status-change-id", recordId: recordExample.id, action: "DEACTIVATE", status: "APPROVED", checker: "checker" },
  }),
] });

collection.item.push({ name: "04 — Administration controller", item: [
  login("Login as maker administrator", "makerUsername"),
  apiRequest({
    name: "Get administration directory",
    method: "GET",
    path: "/api/v1/administration",
    description: "Returns users, groups, grants and security changes visible to an administrator.",
    example: { users: [{ id: "maker", name: "Maya Tan", active: true, groups: ["bullion", "csa"] }], groups: [{ id: "csa", name: "R2WD_GTO_ISTOA_CSA", active: true }], grants: [{ id: "grant-id", groupId: "csa", feature: "administration", action: "ADMIN", scope: "SYSTEM" }], changes: [] },
  }),
  apiRequest({
    name: "Propose administration change",
    method: "POST",
    path: "/api/v1/administration/changes",
    headers: csrfJsonHeaders,
    body: rawJsonBody("{\n  \"kind\": \"GROUP\",\n  \"values\": {\n    \"groupId\": \"{{adminGroupId}}\",\n    \"name\": \"Postman Audit Reviewers\",\n    \"active\": \"true\"\n  },\n  \"reason\": \"Demonstrate administration maker-checker\"\n}"),
    prerequest: ["pm.collectionVariables.set(\"adminGroupId\", `postman-group-${Date.now()}`);"],
    description: "Full SecurityChangeRequest body. kind supports GROUP, MEMBERSHIP and GRANT, each with its own values schema.",
    example: { id: "admin-change-id", kind: "GROUP", values: { groupId: "postman-group", name: "Postman Audit Reviewers", active: "true" }, maker: "maker", checker: null, reason: "Demonstrate administration maker-checker", status: "PENDING", submittedAt: "2026-09-10T06:53:00Z" },
    tests: ["pm.collectionVariables.set(\"adminChangeId\", pm.response.json().id);"],
  }),
  login("Login as checker administrator", "checkerUsername"),
  apiRequest({
    name: "Approve administration change",
    method: "POST",
    path: "/api/v1/administration/changes/:id/decision",
    pathVariables: [pathVar("id", "{{adminChangeId}}", "Security change ID")],
    headers: csrfJsonHeaders,
    body: jsonBody({ approve: true, reason: "Security change independently approved" }),
    description: "Path variable: id. Uses the common ApprovalDecisionRequest body.",
    example: { id: "admin-change-id", kind: "GROUP", values: { groupId: "postman-group", name: "Postman Audit Reviewers", active: "true" }, maker: "maker", checker: "checker", reason: "Demonstrate administration maker-checker", status: "APPROVED", submittedAt: "2026-09-10T06:53:00Z" },
  }),
] });

collection.item.push({ name: "05 — Import controller", item: [
  login("Login as maker for import", "makerUsername"),
  apiRequest({
    name: "Import feature records from CSV",
    method: "POST",
    path: "/api/v1/features/:feature/import",
    pathVariables: [pathVar("feature", "{{feature}}", "Feature whose CSV header must match its field definition")],
    query: [queryParam("scope", "{{scope}}", "Authorised scope applied to every imported row")],
    headers: csrfHeader,
    body: { mode: "formdata", formdata: [{ key: "file", type: "file", src: "docs/vault-import.csv", description: "UTF-8 CSV, maximum 2 MB and 100 rows" }] },
    description: "Path variable: feature; query parameter: scope; multipart part: file. In Postman, reselect docs/vault-import.csv if local file access is not retained after import.",
    example: { batchId: "import-batch-id", submitted: 1, changeIds: ["change-1"] },
  }),
] });

const auditQuery = [
  queryParam("category", "STATIC", "Optional audit category", true),
  queryParam("feature", "{{feature}}", "Optional feature key", true),
  queryParam("record", "{{recordId}}", "Optional record ID or business reference", true),
  queryParam("field", "name", "Optional changed field name", true),
  queryParam("user", "maker", "Optional actor, maker or checker", true),
  queryParam("action", "ADD", "Optional action", true),
  queryParam("status", "APPROVED", "Optional status", true),
  queryParam("reference", "{{changeRequestId}}", "Optional correlation/business reference", true),
  queryParam("screen", "Approval review", "Optional source/screen", true),
  queryParam("scope", "{{scope}}", "Optional authorised scope", true),
  queryParam("from", "2026-09-01", "Optional inclusive ISO date", true),
  queryParam("to", "2026-09-30", "Optional inclusive ISO date", true),
  queryParam("page", "{{page}}", "Zero-based page number"),
  queryParam("size", "{{pageSize}}", "Page size"),
];

collection.item.push({ name: "06 — Audit controller", item: [
  apiRequest({
    name: "Search audit events",
    method: "GET",
    path: "/api/v1/audit",
    query: auditQuery,
    description: "All AuditSearchRequest query parameters are shown. Optional filters are disabled by default; enable only those required. page defaults to 0 and size to 25.",
    example: { content: [auditExample], totalElements: 1, number: 0, size: 25 },
    tests: [
      "const body = pm.response.json();",
      "pm.test(\"Paged response\", () => pm.expect(body.content).to.be.an(\"array\"));",
      "if (body.content.length) pm.collectionVariables.set(\"auditId\", body.content[0].id);",
    ],
  }),
  apiRequest({
    name: "Get audit event detail",
    method: "GET",
    path: "/api/v1/audit/:id",
    pathVariables: [pathVar("id", "{{auditId}}", "Audit event ID returned by search")],
    description: "Path variable: id. Returns immutable evidence including endpoint, trace, actor and before/proposed/after snapshots.",
    example: auditExample,
  }),
] });

const completeFilter = {
  category: "STATIC", feature: "vault", record: null, field: null, user: null,
  action: null, status: null, reference: null, screen: null, scope: "BULLION", from: null, to: null,
};

collection.item.push({ name: "07 — Report and schedule controller", item: [
  apiRequest({
    name: "Create audit report",
    method: "POST",
    path: "/api/v1/reports",
    headers: csrfJsonHeaders,
    body: jsonBody({ format: "CSV", filter: completeFilter }),
    description: "Full GenerateReportRequest. format supports CSV or XLSX; filter uses every AuditSearchRequest field.",
    status: 202,
    example: reportExample,
    tests: ["pm.collectionVariables.set(\"reportId\", pm.response.json().id);"],
  }),
  apiRequest({
    name: "List report jobs",
    method: "GET",
    path: "/api/v1/reports",
    description: "Lists report jobs the actor is permitted to download. The asynchronous worker changes REQUESTED to READY or FAILED.",
    example: [{ ...reportExample, status: "READY", rowCount: 12 }],
    tests: [
      "const selected = pm.response.json().find(item => item.id === pm.collectionVariables.get(\"reportId\"));",
      "if (selected) console.log(\"Report status:\", selected.status);",
      "const attempt = Number(pm.collectionVariables.get(\"reportPollAttempt\") || 0);",
      "if (selected && [\"REQUESTED\", \"RUNNING\"].includes(selected.status) && attempt < 10) {",
      "  pm.collectionVariables.set(\"reportPollAttempt\", String(attempt + 1));",
      "  setTimeout(() => {}, 1000);",
      "  pm.execution.setNextRequest(\"List report jobs\");",
      "} else {",
      "  pm.collectionVariables.unset(\"reportPollAttempt\");",
      "  pm.test(\"Report is ready for download\", () => pm.expect(selected && selected.status).to.eql(\"READY\"));",
      "}",
    ],
  }),
  apiRequest({
    name: "Download generated report",
    method: "GET",
    path: "/api/v1/reports/:id/download",
    pathVariables: [pathVar("id", "{{reportId}}", "Report job ID; job must be READY and unexpired")],
    description: "Path variable: id. Returns a no-store attachment. If the worker has not completed, retry after GET /reports reports READY.",
    example: "id,eventType,category,feature,scope,action,status,actor,occurredAt\nexample,DOMAIN,STATIC,vault,BULLION,ADD,APPROVED,checker,2026-09-10T06:51:00Z\n",
    exampleContentType: "text/csv;charset=UTF-8",
  }),
  apiRequest({
    name: "List report schedules",
    method: "GET",
    path: "/api/v1/schedules",
    description: "Lists schedules visible to the current actor.",
    example: [scheduleExample],
  }),
  apiRequest({
    name: "Create report schedule",
    method: "POST",
    path: "/api/v1/schedules",
    headers: csrfJsonHeaders,
    body: jsonBody({ name: "Postman daily Vault audit", category: "STATIC", feature: "vault", scope: "BULLION", format: "CSV", intervalDays: 1, audienceGroup: "csa" }),
    description: "Full CreateScheduleRequest. intervalDays must be 1-365; format supports CSV or XLSX.",
    example: scheduleExample,
    tests: ["pm.collectionVariables.set(\"scheduleId\", pm.response.json().id);"],
  }),
  apiRequest({
    name: "Toggle report schedule",
    method: "POST",
    path: "/api/v1/schedules/:id/toggle",
    pathVariables: [pathVar("id", "{{scheduleId}}", "Report schedule ID")],
    headers: csrfHeader,
    description: "Path variable: id. No request body. Toggles enabled and recalculates the next run when enabled.",
    example: { ...scheduleExample, enabled: false, nextRunAt: null },
  }),
] });

collection.item.push({ name: "08 — Common error responses", item: [
  apiRequest({
    name: "Malformed JSON — SYS-400-001",
    method: "POST",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    headers: csrfJsonHeaders,
    body: rawJsonBody("{\n  \"businessKey\": \"BROKEN\","),
    description: "Framework JSON parsing failure.",
    status: 400,
    example: problem(400, "SYS-400-001", "Some input is invalid. Check the fields and date range, then try again."),
    exampleName: "400 malformed JSON",
    exampleContentType: "application/problem+json",
    tests: ["pm.test(\"System validation code\", () => pm.expect(pm.response.json().code).to.eql(\"SYS-400-001\"));"],
  }),
  apiRequest({
    name: "DTO validation — SYS-400-001",
    method: "POST",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    headers: csrfJsonHeaders,
    body: jsonBody({ businessKey: "", scope: "", values: {}, reason: "" }),
    description: "Bean validation failure showing safe fieldErrors.",
    status: 400,
    example: problem(400, "SYS-400-001", "Some input is invalid. Check the fields and date range, then try again.", { businessKey: "must not be blank", reason: "must not be blank", scope: "must not be blank" }),
    exampleName: "400 field validation",
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Feature validation — WF-400-001",
    method: "POST",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    headers: csrfJsonHeaders,
    body: jsonBody({ businessKey: "VALID-KEY", scope: "BULLION", values: { name: "Missing other required feature values" }, reason: "Feature validation example" }),
    description: "Syntactically valid DTO that violates a feature-owned field definition.",
    status: 400,
    example: problem(400, "WF-400-001", "The workflow request is invalid. Check the feature fields and try again."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Audit query validation — AUD-400-001",
    method: "GET",
    path: "/api/v1/audit",
    query: [queryParam("from", "2026-09-10", "Inclusive start date"), queryParam("to", "2026-09-01", "Inclusive end date")],
    description: "Feature validation failure because from is after to.",
    status: 400,
    example: problem(400, "AUD-400-001", "The audit search criteria are invalid. Check the filters and date range."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Missing resource — AUD-404-001",
    method: "GET",
    path: "/api/v1/audit/:id",
    pathVariables: [pathVar("id", "{{missingAuditId}}", "Nonexistent audit ID")],
    description: "Feature-owned missing-resource response.",
    status: 404,
    example: problem(404, "AUD-404-001", "The requested audit event is not available."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Unknown endpoint — SYS-404-001",
    method: "GET",
    path: "/api/v1/does-not-exist",
    description: "Central handler response for an unmapped endpoint.",
    status: 404,
    example: problem(404, "SYS-404-001", "The requested endpoint was not found."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Unsupported method — SYS-405-001",
    method: "POST",
    path: "/api/v1/features",
    headers: csrfHeader,
    description: "POST is not mapped for the feature catalogue endpoint.",
    status: 405,
    example: problem(405, "SYS-405-001", "The request method is not supported for this endpoint."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Unsupported media type — SYS-415-001",
    method: "POST",
    path: "/api/v1/features/:feature/records",
    pathVariables: [pathVar("feature", "{{feature}}", "Registered feature key")],
    headers: [...csrfHeader, { key: "Content-Type", value: "application/xml", type: "text" }],
    body: { mode: "raw", raw: "<record/>" },
    description: "Controller accepts JSON, not XML.",
    status: 415,
    example: problem(415, "SYS-415-001", "The request content type is not supported."),
    exampleContentType: "application/problem+json",
  }),
  login("Login as reviewer without administration permission", "reviewerUsername"),
  apiRequest({
    name: "Forbidden action — IDN-403-001",
    method: "GET",
    path: "/api/v1/administration",
    description: "Reviewer is authenticated but lacks the administration permission.",
    status: 403,
    example: problem(403, "IDN-403-001", "You are not authorised to perform this action."),
    exampleContentType: "application/problem+json",
  }),
] });

collection.item.push({ name: "09 — Logout, authentication failure and internal contract", item: [
  apiRequest({
    name: "Logout",
    method: "POST",
    path: "/api/v1/logout",
    headers: csrfHeader,
    description: "Spring Security logout endpoint. Invalidates the authenticated session and writes logout audit evidence.",
    status: 204,
    example: "",
  }),
  apiRequest({
    name: "Get fresh anonymous CSRF token",
    method: "GET",
    path: "/api/v1/csrf",
    description: "Creates a fresh anonymous session after logout.",
    example: { headerName: "X-CSRF-TOKEN", token: "example-token" },
    tests: [
      "const body = pm.response.json();",
      "pm.collectionVariables.set(\"csrfHeader\", body.headerName);",
      "pm.collectionVariables.set(\"csrfToken\", body.token);",
    ],
  }),
  apiRequest({
    name: "Invalid credentials — IDN-401-001",
    method: "POST",
    path: "/api/v1/login",
    headers: [...csrfHeader, { key: "Content-Type", value: "application/x-www-form-urlencoded", type: "text" }],
    body: { mode: "urlencoded", urlencoded: [{ key: "username", value: "{{makerUsername}}", type: "text" }, { key: "password", value: "{{wrongPassword}}", type: "text" }] },
    description: "Failed login response. Password and other credentials are never returned or stored as audit metadata.",
    status: 401,
    example: problem(401, "IDN-401-001", "Authentication is required to access this resource."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Unauthenticated protected request — IDN-401-001",
    method: "GET",
    path: "/api/v1/session",
    description: "Protected controller endpoint requested without an authenticated session.",
    status: 401,
    example: problem(401, "IDN-401-001", "Authentication is required to access this resource."),
    exampleContentType: "application/problem+json",
  }),
  apiRequest({
    name: "Internal error contract — saved example only, do not run",
    method: "GET",
    path: "/documentation/internal-error-example",
    description: "Documentation-only request holding the safe SYS-500-001 saved response. The production application deliberately has no endpoint that throws an internal exception. Deselect this request in Collection Runner.",
    status: 500,
    example: problem(500, "SYS-500-001", "We could not complete the request. Contact support with the trace reference."),
    exampleName: "500 safe internal error",
    exampleContentType: "application/problem+json",
  }),
] });

const environment = {
  id: "86796f2c-95cd-4103-8415-1cd6729f6f0c",
  name: "Custody Audit Control — Local Complete API",
  values: [
    { key: "baseUrl", value: "http://127.0.0.1:8091", enabled: true, type: "default" },
    { key: "makerUsername", value: "maker", enabled: true, type: "default" },
    { key: "checkerUsername", value: "checker", enabled: true, type: "default" },
    { key: "reviewerUsername", value: "reviewer", enabled: true, type: "default" },
    { key: "demoPassword", value: "CustodyDemo!2026", enabled: true, type: "secret" },
    { key: "wrongPassword", value: "Definitely-Wrong-Password", enabled: true, type: "secret" },
    { key: "feature", value: "vault", enabled: true, type: "default" },
    { key: "scope", value: "BULLION", enabled: true, type: "default" },
    { key: "page", value: "0", enabled: true, type: "default" },
    { key: "pageSize", value: "25", enabled: true, type: "default" },
    { key: "missingAuditId", value: "AUDIT-EVIDENCE-DOES-NOT-EXIST", enabled: true, type: "default" },
  ],
  _postman_variable_scope: "environment",
  _postman_exported_at: "2026-09-10T08:00:00.000Z",
  _postman_exported_using: "Custody Audit Control generator",
};

fs.writeFileSync(collectionFile, `${JSON.stringify(collection, null, 2)}\n`);
fs.writeFileSync(environmentFile, `${JSON.stringify(environment, null, 2)}\n`);
console.log(`Generated ${collectionFile}`);
console.log(`Generated ${environmentFile}`);
