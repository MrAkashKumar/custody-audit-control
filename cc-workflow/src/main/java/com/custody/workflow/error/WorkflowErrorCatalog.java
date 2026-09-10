package com.custody.workflow.error;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class WorkflowErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition INPUT_INVALID =
      error("WF-400-001", 400, "error.workflow.input-invalid", ErrorCategory.VALIDATION);
  public static final ErrorDefinition SELF_APPROVAL_FORBIDDEN =
      error(
          "WF-403-001", 403, "error.workflow.self-approval-forbidden", ErrorCategory.AUTHORIZATION);
  public static final ErrorDefinition RESOURCE_NOT_AVAILABLE =
      error("WF-404-001", 404, "error.workflow.resource-not-available", ErrorCategory.NOT_FOUND);
  public static final ErrorDefinition DUPLICATE_KEY =
      error("WF-409-001", 409, "error.workflow.duplicate-key", ErrorCategory.CONFLICT);
  public static final ErrorDefinition REQUEST_ALREADY_PENDING =
      error("WF-409-002", 409, "error.workflow.request-already-pending", ErrorCategory.CONFLICT);
  public static final ErrorDefinition STALE_VERSION =
      error("WF-409-003", 409, "error.workflow.stale-version", ErrorCategory.CONFLICT);
  public static final ErrorDefinition INVALID_TRANSITION =
      error("WF-409-004", 409, "error.workflow.invalid-transition", ErrorCategory.CONFLICT);
  public static final ErrorDefinition BUSINESS_RULE_VIOLATION =
      error(
          "WF-422-001", 422, "error.workflow.business-rule-violation", ErrorCategory.BUSINESS_RULE);

  private static final List<ErrorDefinition> DEFINITIONS =
      List.of(
          INPUT_INVALID,
          SELF_APPROVAL_FORBIDDEN,
          RESOURCE_NOT_AVAILABLE,
          DUPLICATE_KEY,
          REQUEST_ALREADY_PENDING,
          STALE_VERSION,
          INVALID_TRANSITION,
          BUSINESS_RULE_VIOLATION);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static ErrorDefinition error(
      String code, int status, String messageKey, ErrorCategory category) {
    return new ErrorDefinition(code, status, messageKey, category, false);
  }
}
