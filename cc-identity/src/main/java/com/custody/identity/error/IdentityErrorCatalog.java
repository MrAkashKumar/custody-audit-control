package com.custody.identity.error;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class IdentityErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition INPUT_INVALID =
      error("IDN-400-001", 400, "error.identity.input-invalid", ErrorCategory.VALIDATION);
  public static final ErrorDefinition AUTHENTICATION_REQUIRED =
      error(
          "IDN-401-001",
          401,
          "error.identity.authentication-required",
          ErrorCategory.AUTHENTICATION);
  public static final ErrorDefinition ACTION_FORBIDDEN =
      error("IDN-403-001", 403, "error.identity.action-forbidden", ErrorCategory.AUTHORIZATION);
  public static final ErrorDefinition SELF_APPROVAL_FORBIDDEN =
      error(
          "IDN-403-002",
          403,
          "error.identity.self-approval-forbidden",
          ErrorCategory.AUTHORIZATION);
  public static final ErrorDefinition RESOURCE_NOT_AVAILABLE =
      error("IDN-404-001", 404, "error.identity.resource-not-available", ErrorCategory.NOT_FOUND);
  public static final ErrorDefinition DUPLICATE_KEY =
      error("IDN-409-001", 409, "error.identity.duplicate-key", ErrorCategory.CONFLICT);
  public static final ErrorDefinition STALE_VERSION =
      error("IDN-409-002", 409, "error.identity.stale-version", ErrorCategory.CONFLICT);
  public static final ErrorDefinition INVALID_TRANSITION =
      error("IDN-409-003", 409, "error.identity.invalid-transition", ErrorCategory.CONFLICT);

  private static final List<ErrorDefinition> DEFINITIONS =
      List.of(
          INPUT_INVALID,
          AUTHENTICATION_REQUIRED,
          ACTION_FORBIDDEN,
          SELF_APPROVAL_FORBIDDEN,
          RESOURCE_NOT_AVAILABLE,
          DUPLICATE_KEY,
          STALE_VERSION,
          INVALID_TRANSITION);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static ErrorDefinition error(
      String code, int status, String messageKey, ErrorCategory category) {
    return new ErrorDefinition(code, status, messageKey, category, false);
  }
}
