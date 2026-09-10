package com.custody.app.constants;

public final class ApiRoutes {
  private ApiRoutes() {}

  public static final String BASE = "/api/v1";
  public static final String CSRF = BASE + "/csrf",
      SESSION = BASE + "/session",
      LOGIN = BASE + "/login",
      LOGOUT = BASE + "/logout";
  public static final String FEATURES = BASE + "/features",
      RECORDS = BASE + "/records",
      CHANGES = BASE + "/changes";
  public static final String AUDIT = BASE + "/audit",
      REPORTS = BASE + "/reports",
      SCHEDULES = BASE + "/schedules",
      ADMIN = BASE + "/administration";
  public static final String FEATURE_RECORDS = FEATURES + "/{feature}/records",
      FEATURE_IMPORT = FEATURES + "/{feature}/import",
      RECORD_EDIT = RECORDS + "/{id}/edit",
      RECORD_STATUS = RECORDS + "/{id}/status",
      CHANGE_DECISION = CHANGES + "/{id}/decision",
      AUDIT_DETAIL = AUDIT + "/{id}",
      REPORT_DOWNLOAD = REPORTS + "/{id}/download",
      SCHEDULE_TOGGLE = SCHEDULES + "/{id}/toggle",
      ADMIN_CHANGES = ADMIN + "/changes",
      ADMIN_CHANGE_DECISION = ADMIN_CHANGES + "/{id}/decision";
  public static final String TRACE_HEADER = "X-Correlation-ID";
}
