package com.custody.app.audit;

public final class AuditRequestAttributes {
  public static final String ERROR_CODE = AuditRequestAttributes.class.getName() + ".errorCode";
  public static final String STARTED_NANOS =
      AuditRequestAttributes.class.getName() + ".startedNanos";

  private AuditRequestAttributes() {}
}
