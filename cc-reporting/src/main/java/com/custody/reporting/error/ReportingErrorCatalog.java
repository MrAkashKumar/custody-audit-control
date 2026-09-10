package com.custody.reporting.error;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class ReportingErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition ACTION_FORBIDDEN =
      error("RPT-403-001", 403, "error.reporting.action-forbidden", ErrorCategory.AUTHORIZATION);
  public static final ErrorDefinition RESOURCE_NOT_AVAILABLE =
      error("RPT-404-001", 404, "error.reporting.resource-not-available", ErrorCategory.NOT_FOUND);
  public static final ErrorDefinition REPORT_NOT_READY =
      error("RPT-409-001", 409, "error.reporting.not-ready", ErrorCategory.CONFLICT);
  public static final ErrorDefinition REPORT_EXPIRED =
      error("RPT-410-001", 410, "error.reporting.expired", ErrorCategory.CONFLICT);
  public static final ErrorDefinition REPORT_TOO_LARGE =
      error("RPT-422-001", 422, "error.reporting.too-large", ErrorCategory.BUSINESS_RULE);

  private static final List<ErrorDefinition> DEFINITIONS =
      List.of(
          ACTION_FORBIDDEN,
          RESOURCE_NOT_AVAILABLE,
          REPORT_NOT_READY,
          REPORT_EXPIRED,
          REPORT_TOO_LARGE);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static ErrorDefinition error(
      String code, int status, String messageKey, ErrorCategory category) {
    return new ErrorDefinition(code, status, messageKey, category, false);
  }
}
