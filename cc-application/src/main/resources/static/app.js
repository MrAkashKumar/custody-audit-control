const API = new URL("./api/v1", document.baseURI).pathname.replace(/\/$/, "");
const routes = {
  csrf: "/csrf",
  session: "/session",
  login: "/login",
  logout: "/logout",
  features: "/features",
  records: "/records",
  changes: "/changes",
  audit: "/audit",
  reports: "/reports",
  schedules: "/schedules",
  admin: "/administration",
};
const $ = (s) => document.querySelector(s),
  esc = (v) =>
    String(v ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
const date = (v) =>
  v
    ? new Intl.DateTimeFormat("en-SG", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Singapore",
      }).format(new Date(v))
    : "—";
const badge = (v) =>
  `<span class="badge ${esc(String(v).toLowerCase())}">${esc(v)}</span>`;
let csrf,
  session,
  features = [],
  page = "audit",
  feature = "vault",
  records = [],
  changes = [],
  auditPage = 0,
  auditCategory = "STATIC",
  filter = {},
  noticeTimer;
async function token() {
  csrf = await fetch(API + routes.csrf, { credentials: "same-origin" }).then(
    (r) => r.json(),
  );
}
async function api(path, options = {}) {
  const headers = {
    ...(options.body ? { "Content-Type": "application/json" } : {}),
    ...(csrf ? { [csrf.headerName]: csrf.token } : {}),
    ...options.headers,
  };
  const r = await fetch(API + path, {
    credentials: "same-origin",
    ...options,
    headers,
  });
  if (!r.ok) {
    let e;
    try {
      e = await r.json();
    } catch {
      e = { message: "The request could not be completed." };
    }
    if (r.status === 401 && page !== "login") {
      showLogin();
    }
    throw new Error(
      `${e.message || "Request failed."}${
        e.fieldErrors && Object.keys(e.fieldErrors).length
          ? "\n" +
            Object.entries(e.fieldErrors)
              .map(([k, v]) => k + ": " + v)
              .join("\n")
          : ""
      }\nReference: ${e.traceId || r.headers.get("X-Correlation-ID") || "unavailable"}`,
    );
  }
  return r.status === 204
    ? null
    : r.headers.get("content-type")?.includes("json")
      ? r.json()
      : r;
}
const send = (path, body) =>
  api(path, { method: "POST", body: JSON.stringify(body) });
function notice(message, error = false) {
  clearTimeout(noticeTimer);
  $("#notice").textContent = message;
  $("#notice").className = error ? "error" : "";
  $("#notice").hidden = false;
  noticeTimer = setTimeout(
    () => ($("#notice").hidden = true),
    error ? 15000 : 5000,
  );
}
function fail(e) {
  notice(e.message, true);
}
function showLogin() {
  page = "login";
  $("#login").hidden = false;
  $("#workspace").hidden = true;
}
function allowed(action, key = feature, scope) {
  return session.grants.some(
    (g) =>
      g.action === action &&
      (g.feature === "*" || g.feature === key) &&
      (!scope || g.scope === scope),
  );
}
function scopeOptions(action, key = feature) {
  return [
    ...new Set(
      session.grants
        .filter(
          (g) =>
            g.action === action && (g.feature === "*" || g.feature === key),
        )
        .map((g) => g.scope),
    ),
  ]
    .map((x) => `<option>${esc(x)}</option>`)
    .join("");
}
function featureOptions(all = true) {
  return (
    `${all ? '<option value="">All setup types</option>' : ""}` +
    features
      .map((f) => `<option value="${esc(f.key)}">${esc(f.label)}</option>`)
      .join("")
  );
}
function heading(title, description, actions = "") {
  return `<div class="heading"><div><span class="eyebrow">CONTROL & EVIDENCE</span><h1>${title}</h1><p>${description}</p></div><div class="actions">${actions}</div></div>`;
}
function empty(
  title = "No matching records",
  text = "Change your filters or submit a new record.",
) {
  return `<div class="empty"><h3>${title}</h3>${text}</div>`;
}
function modal(title, body) {
  $("#dialog-title").textContent = title;
  $("#dialog-body").innerHTML = body;
  $("#dialog").showModal();
}
$("#close-dialog").onclick = () => $("#dialog").close();
async function start() {
  session = await api(routes.session);
  features = await api(routes.features);
  $("#who").textContent =
    `${session.name} · ${session.groups.map((g) => g.name).join(", ")}`;
  $("#feature-nav").innerHTML = features
    .map((f) => `<button data-feature="${esc(f.key)}">${esc(f.label)}</button>`)
    .join("");
  $("#admin-nav").hidden = !allowed("ADMIN", "administration", "SYSTEM");
  $("#login").hidden = true;
  $("#workspace").hidden = false;
  page = "audit";
  await render();
}
$("#sign-in").onsubmit = async (e) => {
  e.preventDefault();
  const b = e.submitter;
  b.disabled = true;
  try {
    await token();
    await api(routes.login, {
      method: "POST",
      body: new URLSearchParams(new FormData(e.target)),
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
    });
    await token();
    await start();
    e.target.reset();
  } catch (e) {
    fail(e);
  } finally {
    b.disabled = false;
  }
};
$("#sign-out").onclick = async () => {
  try {
    await api(routes.logout, { method: "POST" });
    showLogin();
    await token();
  } catch (e) {
    fail(e);
  }
};
document.addEventListener("click", async (e) => {
  const b = e.target.closest("[data-page],[data-feature]");
  if (!b) return;
  page = b.dataset.page || "feature";
  if (b.dataset.feature) feature = b.dataset.feature;
  try {
    await render();
  } catch (e) {
    fail(e);
  }
});
async function render() {
  document
    .querySelectorAll("nav button")
    .forEach((b) =>
      b.classList.toggle(
        "active",
        b.dataset.page === page ||
          (page === "feature" && b.dataset.feature === feature),
      ),
    );
  changes = await api(routes.changes);
  $("#pending-count").textContent = changes.length || "";
  $("#breadcrumb").textContent =
    "Workspace / " +
    (page === "feature"
      ? features.find((f) => f.key === feature)?.label
      : {
          audit: "Audit trail",
          approvals: "Pending approvals",
          reports: "Reports & schedules",
          admin: "Users & groups",
        }[page]);
  if (page === "feature") await renderFeature();
  else if (page === "approvals") renderApprovals();
  else if (page === "audit") await renderAudit();
  else if (page === "reports") await renderReports();
  else if (page === "admin") await renderAdmin();
}
async function renderFeature() {
  const f = features.find((f) => f.key === feature);
  records = await api(`${routes.features}/${feature}/records`);
  $("#content").innerHTML =
    heading(
      esc(f.label),
      "Approved records stay in effect while proposed changes are reviewed.",
      allowed("ADD")
        ? '<button id="import-records">Import CSV</button><button class="primary" id="add-record">+ Add record</button>'
        : "",
    ) +
    `<section class="panel"><div class="panel-head"><h3>${records.length} approved records</h3><span class="scope-label">Active / Inactive is separate from approval status</span></div>${
      records.length
        ? `<div class="table-wrap"><table><thead><tr><th>Identifier / name</th><th>Scope</th><th>Record status</th><th>Pending request</th><th>Last approved change</th><th>Actions</th></tr></thead><tbody>${records
            .map((r) => {
              const pending = changes.find((c) => c.recordId === r.id);
              return `<tr><td>${esc(r.businessKey)}<small>${esc(r.values.name)}</small></td><td>${esc(r.scope)}</td><td>${badge(r.status)}</td><td>${pending ? badge("PENDING") : "—"}</td><td>${date(r.updatedAt)}</td><td><div class="actions"><button data-view="${r.id}">View</button>${allowed("EDIT", feature, r.scope) ? `<button data-edit="${r.id}" ${pending ? "disabled" : ""}>Edit</button>` : ""}${allowed("STATUS", feature, r.scope) ? `<button data-status="${r.id}" ${pending ? "disabled" : ""}>${r.status === "ACTIVE" ? "Deactivate" : "Activate"}</button>` : ""}</div></td></tr>`;
            })
            .join("")}</tbody></table></div>`
        : empty(
            "No approved records yet",
            "Submit an addition, then ask another authorised user to approve it.",
          )
    }</section>`;
  $("#add-record")?.addEventListener("click", () => recordForm());
  $("#import-records")?.addEventListener("click", importDialog);
  $("#content").onclick = (e) => {
    const b = e.target.closest("[data-edit],[data-view],[data-status]");
    if (!b) return;
    const r = records.find(
      (r) => r.id === (b.dataset.edit || b.dataset.view || b.dataset.status),
    );
    if (b.dataset.edit) recordForm(r);
    if (b.dataset.view)
      modal(
        r.businessKey,
        `<div class="meta-grid">${Object.entries(r.values)
          .map(([k, v]) => `<div><small>${esc(k)}</small>${esc(v)}</div>`)
          .join("")}</div><p>${badge(r.status)} · ${esc(r.scope)}</p>`,
      );
    if (b.dataset.status) statusForm(r);
  };
}
function recordForm(record) {
  const f = features.find((f) => f.key === feature);
  modal(
    record ? "Propose changes" : "Add " + f.label,
    `<p class="muted">This proposal takes effect after independent approval.</p><form id="record-form"><div class="form-grid">${record ? "" : `<label>Identifier<input name="businessKey" pattern="[A-Za-z0-9_-]{2,60}" required></label><label>Scope<select name="scope">${scopeOptions("ADD")}</select></label>`}${f.fields.map((field) => `<label>${esc(field.label)}<input name="${esc(field.key)}" type="${field.type === "date" ? "date" : field.type === "decimal" ? "number" : "text"}" ${field.type === "decimal" ? 'min="0" step="any"' : ""} maxlength="240" ${field.required ? "required" : ""} value="${esc(record?.values[field.key] || "")}"></label>`).join("")}<label class="wide">Reason<textarea name="reason" maxlength="1000" required></textarea></label></div><button class="primary">Submit for approval</button></form>`,
  );
  $("#record-form").onsubmit = async (e) => {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.target));
    const values = Object.fromEntries(f.fields.map((x) => [x.key, d[x.key]]));
    e.submitter.disabled = true;
    try {
      if (record)
        await send(`${routes.records}/${record.id}/edit`, {
          version: record.version,
          values,
          reason: d.reason,
        });
      else
        await send(`${routes.features}/${feature}/records`, {
          businessKey: d.businessKey,
          scope: d.scope,
          values,
          reason: d.reason,
        });
      $("#dialog").close();
      notice("Submitted for independent approval.");
      await render();
    } catch (err) {
      fail(err);
      e.submitter.disabled = false;
    }
  };
}
function statusForm(r) {
  const status = r.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
  modal(
    `Request ${status.toLowerCase()} status`,
    `<p>${esc(r.businessKey)} remains ${esc(r.status.toLowerCase())} until approved.</p><form id="status-form"><label>Reason<textarea name="reason" required maxlength="1000"></textarea></label><button class="primary">Submit status change</button></form>`,
  );
  $("#status-form").onsubmit = async (e) => {
    e.preventDefault();
    try {
      await send(`${routes.records}/${r.id}/status`, {
        version: r.version,
        status,
        reason: new FormData(e.target).get("reason"),
      });
      $("#dialog").close();
      await render();
      notice("Status change submitted.");
    } catch (err) {
      fail(err);
    }
  };
}
function diff(before, after, proposed = null) {
  const keys = proposed
    ? [...new Set([...Object.keys(before), ...Object.keys(proposed), ...Object.keys(after)])]
    : [...new Set([...Object.keys(before), ...Object.keys(after)])];
  const headings = proposed
    ? "<th>Before</th><th>Proposed</th><th>After / effective</th>"
    : "<th>Before / current</th><th>After / proposed</th>";
  return `<div class="table-wrap"><table><thead><tr><th>Field</th>${headings}</tr></thead><tbody>${keys
    .sort()
    .map(
      (k) =>
        `<tr><td>${esc(k)}</td><td>${esc(before[k] ?? "—")}</td>${proposed ? `<td>${esc(proposed[k] ?? "—")}</td>` : ""}<td>${esc(after[k] ?? "—")}</td></tr>`,
    )
    .join("")}</tbody></table></div>`;
}
function renderApprovals() {
  $("#content").innerHTML =
    heading(
      "Pending approvals",
      "Review proposed values before they become operational.",
    ) +
    `<div class="panel">${changes.length ? `<div class="table-wrap"><table><thead><tr><th>Record / feature</th><th>Action</th><th>Maker</th><th>Submitted</th><th>Status</th><th></th></tr></thead><tbody>${changes.map((c) => `<tr><td>${esc(c.businessKey)}<small>${esc(c.feature)}</small></td><td>${esc(c.action)}</td><td>${esc(c.maker)}</td><td>${date(c.submittedAt)}</td><td>${badge(c.status)}</td><td><button data-review="${c.id}">Review</button></td></tr>`).join("")}</tbody></table></div>` : empty("No pending approvals", "New submissions will appear here.")}</div>`;
  $("#content").onclick = (e) => {
    const b = e.target.closest("[data-review]");
    if (b) review(changes.find((c) => c.id === b.dataset.review));
  };
}
function review(c) {
  const can =
    allowed("APPROVE", c.feature, c.scope) && c.maker !== session.user;
  modal(
    "Review " + c.businessKey,
    `<div class="meta-grid"><div><small>Maker</small>${esc(c.maker)}</div><div><small>Action</small>${esc(c.action)}</div><div><small>Submitted</small>${date(c.submittedAt)}</div></div><p><small>Reason</small><br>${esc(c.reason)}</p>${diff(c.before, c.proposed)}${can ? '<form id="decision-form"><label>Decision reason<textarea name="reason" maxlength="1000" required></textarea></label><div class="actions"><button name="decision" value="approve" class="primary">Approve & apply</button><button name="decision" value="reject">Reject proposal</button></div></form>' : '<p class="muted">Another authorised user must approve this proposal.</p>'}`,
  );
  $("#decision-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      await send(`${routes.changes}/${c.id}/decision`, {
        approve: e.submitter.value === "approve",
        reason: new FormData(e.target).get("reason"),
      });
      $("#dialog").close();
      await render();
      notice("Decision recorded with audit evidence.");
    } catch (err) {
      fail(err);
    }
  });
}
async function renderAudit() {
  const params = new URLSearchParams(
    Object.entries({
      ...filter,
      category: auditCategory,
      page: auditPage,
      size: 20,
    }).filter(([, v]) => v !== "" && v != null),
  );
  const data = await api(routes.audit + "?" + params);
  $("#content").innerHTML =
    heading(
      "Audit trail",
      "Search controlled evidence of what changed, who changed it and when.",
      '<button id="export-audit" class="primary">Generate report ↓</button>',
    ) +
    `<div class="tabs">${[
      ["STATIC", "Static-data changes"],
      ["BUSINESS", "Business activity"],
      ["ADMIN", "Administrator activity"],
      ["SECURITY", "Security activity"],
    ]
      .map(
        ([k, v]) =>
          `<button data-category="${k}" class="${auditCategory === k ? "active" : ""}">${v}</button>`,
      )
      .join(
        "",
      )}</div><section class="panel"><form id="filters" class="panel-body filters"><label>Setup / feature<select name="feature">${featureOptions()}<option value="administration">Administration</option></select></label><label>Record / account<input name="record" placeholder="Identifier"></label><label>Field<input name="field" placeholder="e.g. name"></label><label>User<input name="user" placeholder="Maker, checker or actor"></label><label>From date<input name="from" type="date"></label><label>To date<input name="to" type="date"></label><label>Action<select name="action"><option value="">All actions</option>${["ADD", "EDIT", "ACTIVATE", "DEACTIVATE", "MEMBERSHIP", "GRANT", "GROUP", "EXPORT", "IMPORT", "SCHEDULE", "API_REQUEST", "AUTHENTICATION_SUCCESS", "AUTHENTICATION_FAILURE", "LOGOUT", "ACCESS_DENIED", "VALIDATION_FAILURE", "APPLICATION_ERROR", "SUSPICIOUS_ACTIVITY"].map((x) => `<option>${x}</option>`).join("")}</select></label><label>Status<select name="status"><option value="">All statuses</option>${["PENDING", "APPROVED", "REJECTED", "GENERATED", "REQUESTED", "FAILED", "SUCCESS", "FAILURE"].map((x) => `<option>${x}</option>`).join("")}</select></label><label>Reference<input name="reference"></label><label>Screen / endpoint<input name="screen"></label><label>Scope<input name="scope"></label><div class="actions"><button class="primary">Search audit records</button><button type="button" id="reset-filters">Reset</button></div></form></section><section class="panel"><div class="panel-head"><h3>${data.totalElements} matching events</h3><span class="scope-label">Read-only evidence · Your authorised scope</span></div>${data.content.length ? `<div class="table-wrap"><table><thead><tr><th>Time</th><th>Record / activity</th><th>Action</th><th>Maker / checker</th><th>Status</th><th></th></tr></thead><tbody>${data.content.map((e) => `<tr><td>${date(e.occurredAt)}</td><td>${esc(e.recordId)}<small>${esc(e.feature)}</small></td><td>${esc(e.action)}</td><td>${esc(e.maker)}<small>${esc(e.checker || "Awaiting decision")}</small></td><td>${badge(e.status)}</td><td><button data-event="${e.id}">View evidence</button></td></tr>`).join("")}</tbody></table></div>` : empty("No matching evidence", "Adjust your filters or submit and approve a setup change.")}<div class="pagination"><span>Page ${auditPage + 1} · 20 events per page</span><div class="actions"><button id="prev" ${auditPage === 0 ? "disabled" : ""}>Previous</button><button id="next" ${(auditPage + 1) * 20 >= data.totalElements ? "disabled" : ""}>Next</button></div></div></section>`;
  for (const [k, v] of Object.entries(filter))
    if ($("#filters").elements[k]) $("#filters").elements[k].value = v;
  $("#filters").onsubmit = async (e) => {
    e.preventDefault();
    filter = Object.fromEntries(new FormData(e.target));
    auditPage = 0;
    try {
      await renderAudit();
    } catch (e) {
      fail(e);
    }
  };
  $("#reset-filters").onclick = () => {
    filter = {};
    auditPage = 0;
    renderAudit().catch(fail);
  };
  $("#prev").onclick = () => {
    auditPage--;
    renderAudit().catch(fail);
  };
  $("#next").onclick = () => {
    auditPage++;
    renderAudit().catch(fail);
  };
  $("#export-audit").onclick = () => exportDialog();
  $("#content").onclick = async (e) => {
    const tab = e.target.closest("[data-category]");
    if (tab) {
      auditCategory = tab.dataset.category;
      auditPage = 0;
      await renderAudit();
    }
    const b = e.target.closest("[data-event]");
    if (b)
      try {
        const ev = await api(routes.audit + "/" + b.dataset.event);
        modal(
          "Audit evidence",
          `<p>${badge(ev.status)} ${esc(ev.eventType || ev.action)} · ${esc(ev.recordId)}</p><div class="meta-grid"><div><small>Actor</small>${esc(ev.actor)}</div><div><small>Maker / checker</small>${esc(ev.maker)} / ${esc(ev.checker || "—")}</div><div><small>Event time</small>${date(ev.occurredAt)}</div><div><small>Endpoint</small>${esc(ev.httpMethod || "—")} ${esc(ev.endpoint || "—")}</div><div><small>HTTP / outcome</small>${esc(ev.httpStatus ?? "—")} / ${esc(ev.outcome || ev.status)}</div><div><small>Risk / error</small>${esc(ev.riskLevel || "—")} / ${esc(ev.errorCode || "—")}</div></div><p>${esc(ev.reason)}</p>${diff(ev.before, ev.after, ev.proposed)}<div class="timeline"><div><small>Reference</small><span class="mono">${esc(ev.reference)}</span></div><div><small>Trace ID</small><span class="mono">${esc(ev.traceId)}</span></div></div>`,
        );
      } catch (err) {
        fail(err);
      }
  };
}
function exportDialog() {
  modal(
    "Generate secure report",
    `<p class="muted">All matching authorised events are included. The generated file will appear in Reports.</p><form id="export-form"><label>Format<select name="format"><option>CSV</option><option>XLSX</option></select></label><p><small>Criteria</small><br>${esc(JSON.stringify({ ...filter, category: auditCategory }))}</p><button class="primary">Generate report</button></form>`,
  );
  $("#export-form").onsubmit = async (e) => {
    e.preventDefault();
    try {
      const f = Object.fromEntries(
        Object.entries({ ...filter, category: auditCategory }).filter(
          ([, v]) => v !== "",
        ),
      );
      await send(routes.reports, {
        format: new FormData(e.target).get("format"),
        filter: f,
      });
      $("#dialog").close();
      page = "reports";
      await render();
      notice("Report requested. It will be ready for secure download shortly.");
    } catch (err) {
      fail(err);
    }
  };
}
async function renderReports() {
  const [jobs, schedules] = await Promise.all([
    api(routes.reports),
    api(routes.schedules),
  ]);
  $("#content").innerHTML =
    heading(
      "Reports & schedules",
      "Generated evidence is available only to currently authorised users.",
      '<button id="refresh-reports">Refresh</button><button id="new-schedule" class="primary">+ Schedule report</button>',
    ) +
    `<section class="panel"><div class="panel-head"><h3>Secure downloads</h3><span class="scope-label">CSV / Excel · Files expire after the configured period</span></div>${jobs.length ? `<div class="table-wrap"><table><thead><tr><th>Report</th><th>Created</th><th>Source</th><th>Events</th><th>Status</th><th>Expires</th><th></th></tr></thead><tbody>${jobs.map((j) => `<tr><td>${j.format} evidence<small>${esc(j.owner)}${j.audienceGroup ? " · " + esc(j.audienceGroup) : ""}</small></td><td>${date(j.requestedAt)}</td><td>${j.trigger}</td><td>${j.status === "READY" ? j.rowCount : "—"}</td><td>${badge(j.status)}${j.failureCode ? `<small>${esc(j.failureCode)}</small>` : ""}</td><td>${date(j.expiresAt)}</td><td><button data-download="${j.id}" ${j.status !== "READY" ? "disabled" : ""}>Download</button></td></tr>`).join("")}</tbody></table></div>` : empty("No reports yet", "Generate a report from Audit trail or create a schedule.")}</section><section class="panel"><div class="panel-head"><h3>Scheduled reports</h3><span class="scope-label">Singapore midnight · Includes no-activity reports</span></div>${schedules.length ? `<div class="table-wrap"><table><thead><tr><th>Schedule</th><th>Feature</th><th>Frequency</th><th>Next run</th><th>Status</th><th></th></tr></thead><tbody>${schedules.map((s) => `<tr><td>${esc(s.name)}<small>${esc(s.audienceGroup || "Private to owner")}</small></td><td>${esc(s.feature)}</td><td>Every ${s.intervalDays} day(s)</td><td>${date(s.nextRunAt)}</td><td>${badge(s.enabled ? "ACTIVE" : "INACTIVE")}${s.lastFailure ? `<small>${esc(s.lastFailure)}</small>` : ""}</td><td><button data-toggle="${s.id}">${s.enabled ? "Pause" : "Enable"}</button></td></tr>`).join("")}</tbody></table></div>` : empty("No schedules", "Schedule daily evidence for yourself or an authorised group.")}</section>`;
  $("#refresh-reports").onclick = () => renderReports().catch(fail);
  $("#new-schedule").onclick = scheduleDialog;
  $("#content").onclick = async (e) => {
    const b = e.target.closest("[data-download],[data-toggle]");
    if (!b) return;
    try {
      if (b.dataset.toggle) {
        await send(`${routes.schedules}/${b.dataset.toggle}/toggle`, {});
        await renderReports();
      } else {
        const response = await api(
          `${routes.reports}/${b.dataset.download}/download`,
        );
        const url = URL.createObjectURL(await response.blob());
        const a = document.createElement("a");
        a.href = url;
        a.download = `audit-${b.dataset.download}.${jobs.find((j) => j.id === b.dataset.download).format.toLowerCase()}`;
        a.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        notice("Secure download started.");
      }
    } catch (err) {
      fail(err);
    }
  };
}
function scheduleDialog() {
  modal(
    "Schedule a report",
    `<form id="schedule-form" class="form-grid"><label class="wide">Name<input name="name" required maxlength="100"></label><label>Feature<select name="feature">${featureOptions(false)}<option value="administration">Administration</option></select></label><label>Category<select name="category"><option value="STATIC">Static data</option><option value="BUSINESS">Business activity</option><option value="ADMIN">Administrator activity</option></select></label><label>Scope<select name="scope">${[...new Set(session.grants.filter((g) => g.action === "SCHEDULE").map((g) => g.scope))].map((s) => `<option>${esc(s)}</option>`).join("")}</select></label><label>Format<select name="format"><option>CSV</option><option>XLSX</option></select></label><label>Every N days<input type="number" name="intervalDays" min="1" max="365" value="1" required></label><label>Audience<select name="audienceGroup"><option value="">Private to me</option>${session.groups.map((g) => `<option value="${esc(g.id)}">${esc(g.name)}</option>`).join("")}</select></label><button class="primary wide">Create schedule</button></form>`,
  );
  $("#schedule-form").onsubmit = async (e) => {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.target));
    d.intervalDays = Number(d.intervalDays);
    try {
      await send(routes.schedules, d);
      $("#dialog").close();
      await renderReports();
      notice("Schedule created.");
    } catch (err) {
      fail(err);
    }
  };
}
async function renderAdmin() {
  const d = await api(routes.admin);
  $("#content").innerHTML =
    heading(
      "Users & groups",
      "Membership and permission changes require another administrator’s approval.",
      '<button id="group-change">Manage group</button><button id="grant-change">Manage permissions</button><button id="membership-change" class="primary">Change membership</button>',
    ) +
    `<div class="split"><section class="panel"><div class="panel-head"><h3>Users</h3></div><table><thead><tr><th>User</th><th>Memberships</th></tr></thead><tbody>${d.users.map((u) => `<tr><td>${esc(u.name)}<small>${esc(u.id)}</small></td><td>${u.groups.map((g) => esc(d.groups.find((x) => x.id === g)?.name || g)).join("<br>")}</td></tr>`).join("")}</tbody></table></section><section class="panel"><div class="panel-head"><h3>Groups</h3></div><table><thead><tr><th>Group</th><th>Status</th></tr></thead><tbody>${d.groups.map((g) => `<tr><td>${esc(g.name)}<small>${esc(g.id)}</small></td><td>${badge(g.active ? "ACTIVE" : "INACTIVE")}</td></tr>`).join("")}</tbody></table></section></div><section class="panel"><div class="panel-head"><h3>Administration requests</h3></div>${d.changes.length ? `<table><thead><tr><th>Change</th><th>Maker</th><th>Submitted</th><th>Status</th><th></th></tr></thead><tbody>${d.changes.map((c) => `<tr><td>${esc(c.kind)}<small>${esc(c.reason)}</small></td><td>${esc(c.maker)}</td><td>${date(c.submittedAt)}</td><td>${badge(c.status)}</td><td><button data-admin-review="${c.id}">View</button></td></tr>`).join("")}</tbody></table>` : empty("No administration requests", "Changes to memberships and permissions will appear here.")}</section>`;
  $("#membership-change").onclick = () => adminForm("MEMBERSHIP", d);
  $("#group-change").onclick = () => adminForm("GROUP", d);
  $("#grant-change").onclick = () => adminForm("GRANT", d);
  $("#content").onclick = (e) => {
    const b = e.target.closest("[data-admin-review]");
    if (!b) return;
    const c = d.changes.find((c) => c.id === b.dataset.adminReview);
    modal(
      "Administration request",
      diff({}, c.values) +
        `<p>Maker: ${esc(c.maker)} · ${badge(c.status)}</p>${c.status === "PENDING" && c.maker !== session.user ? '<form id="admin-decision"><label>Reason<textarea name="reason" required></textarea></label><div class="actions"><button value="approve" class="primary">Approve</button><button value="reject">Reject</button></div></form>' : ""}`,
    );
    $("#admin-decision")?.addEventListener("submit", async (e) => {
      e.preventDefault();
      try {
        await send(`${routes.admin}/changes/${c.id}/decision`, {
          approve: e.submitter.value === "approve",
          reason: new FormData(e.target).get("reason"),
        });
        $("#dialog").close();
        session = await api(routes.session);
        await renderAdmin();
        notice("Administration decision recorded.");
      } catch (err) {
        fail(err);
      }
    });
  };
}
function adminForm(kind, d) {
  let fields = "";
  const groups = `<label>Group<select name="groupId">${d.groups.map((g) => `<option value="${esc(g.id)}">${esc(g.name)}</option>`).join("")}</select></label>`;
  if (kind === "MEMBERSHIP")
    fields = `<label>User<select name="userId">${d.users.map((u) => `<option value="${esc(u.id)}">${esc(u.name)}</option>`).join("")}</select></label>${groups}<label>Operation<select name="operation"><option>ADD</option><option>REMOVE</option></select></label>`;
  if (kind === "GROUP")
    fields =
      '<label>Group ID (existing or new)<input name="groupId" required pattern="[A-Za-z0-9_-]{2,60}"></label><label>Name<input name="name" required></label><label>Status<select name="active"><option value="true">Active</option><option value="false">Inactive</option></select></label>';
  if (kind === "GRANT")
    fields = `${groups}<label>Feature<select name="feature">${featureOptions(false)}<option value="administration">Administration</option></select></label><label>Action<select name="action">${["VIEW", "ADD", "EDIT", "STATUS", "APPROVE", "AUDIT", "GENERATE", "DOWNLOAD", "SCHEDULE", "ADMIN"].map((a) => `<option>${a}</option>`).join("")}</select></label><label>Scope<input name="scope" required placeholder="BULLION or SYSTEM"></label><input type="hidden" name="operation" value="ADD"><details><summary>Remove an existing permission</summary><label>Grant to remove<select name="grantId"><option value="">Create the grant above</option>${d.grants.map((g) => `<option value="${g.id}">${esc(g.groupId + " / " + g.feature + " / " + g.action + " / " + g.scope)}</option>`).join("")}</select></label></details>`;
  modal(
    "Propose " + kind.toLowerCase() + " change",
    `<form id="admin-form">${fields}<label>Reason<textarea name="reason" required maxlength="1000"></textarea></label><button class="primary">Submit for approval</button></form>`,
  );
  $("#admin-form").onsubmit = async (e) => {
    e.preventDefault();
    const values = Object.fromEntries(new FormData(e.target));
    const reason = values.reason;
    delete values.reason;
    if (kind === "GRANT") {
      if (values.grantId) {
        const grantId = values.grantId;
        Object.keys(values).forEach((k) => delete values[k]);
        Object.assign(values, { operation: "REMOVE", grantId });
      } else delete values.grantId;
    }
    try {
      await send(routes.admin + "/changes", { kind, values, reason });
      $("#dialog").close();
      await renderAdmin();
      notice("Submitted for another administrator’s approval.");
    } catch (err) {
      fail(err);
    }
  };
}
await token();
try {
  await start();
} catch {
  showLogin();
}

function importDialog() {
  const f = features.find((x) => x.key === feature);
  modal(
    "Import additions",
    `<p>Each imported row becomes a separate proposal. No operational values change until approved. The batch is all-or-nothing, up to 100 rows.</p><p class="mono">CSV header: businessKey,reason,${f.fields.map((x) => esc(x.key)).join(",")}</p><form id="import-form"><label>Scope<select name="scope">${scopeOptions("ADD")}</select></label><label>CSV file<input type="file" name="file" accept=".csv,text/csv" required></label><button class="primary">Submit import for approval</button></form>`,
  );
  $("#import-form").onsubmit = async (e) => {
    e.preventDefault();
    try {
      const data = new FormData(e.target);
      const res = await fetch(
        API +
          routes.features +
          "/" +
          feature +
          "/import?scope=" +
          encodeURIComponent(data.get("scope")),
        {
          method: "POST",
          credentials: "same-origin",
          headers: { [csrf.headerName]: csrf.token },
          body: data,
        },
      );
      const result = await res.json();
      if (!res.ok)
        throw new Error(result.message + " · Reference: " + result.traceId);
      $("#dialog").close();
      await render();
      notice(
        result.submitted + " proposals submitted. Batch " + result.batchId,
      );
    } catch (err) {
      fail(err);
    }
  };
}
