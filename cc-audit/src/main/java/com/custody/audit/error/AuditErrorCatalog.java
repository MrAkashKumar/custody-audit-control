package com.custody.audit.error;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class AuditErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition INPUT_INVALID =
      error("AUD-400-001", 400, "error.audit.input-invalid", ErrorCategory.VALIDATION);
  public static final ErrorDefinition RESOURCE_NOT_AVAILABLE =
      error("AUD-404-001", 404, "error.audit.resource-not-available", ErrorCategory.NOT_FOUND);

  private static final List<ErrorDefinition> DEFINITIONS =
      List.of(INPUT_INVALID, RESOURCE_NOT_AVAILABLE);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static ErrorDefinition error(
      String code, int status, String messageKey, ErrorCategory category) {
    return new ErrorDefinition(code, status, messageKey, category, false);
  }
}
