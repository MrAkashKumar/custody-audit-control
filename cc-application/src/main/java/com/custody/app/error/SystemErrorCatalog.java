package com.custody.app.error;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class SystemErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition INPUT_INVALID =
      error("SYS-400-001", 400, "error.system.input-invalid", ErrorCategory.VALIDATION, false);
  public static final ErrorDefinition ENDPOINT_NOT_FOUND =
      error("SYS-404-001", 404, "error.system.endpoint-not-found", ErrorCategory.NOT_FOUND, false);
  public static final ErrorDefinition REQUEST_METHOD_NOT_ALLOWED =
      error("SYS-405-001", 405, "error.system.method-not-allowed", ErrorCategory.VALIDATION, false);
  public static final ErrorDefinition DUPLICATE_KEY =
      error("SYS-409-001", 409, "error.system.duplicate-key", ErrorCategory.CONFLICT, false);
  public static final ErrorDefinition OPTIMISTIC_LOCK_CONFLICT =
      error(
          "SYS-409-002",
          409,
          "error.system.optimistic-lock-conflict",
          ErrorCategory.CONFLICT,
          false);
  public static final ErrorDefinition MEDIA_TYPE_NOT_SUPPORTED =
      error(
          "SYS-415-001",
          415,
          "error.system.media-type-not-supported",
          ErrorCategory.VALIDATION,
          false);
  public static final ErrorDefinition INTERNAL_ERROR =
      error("SYS-500-001", 500, "error.system.internal", ErrorCategory.INTERNAL, false);
  public static final ErrorDefinition PERSISTENCE_UNAVAILABLE =
      error(
          "SYS-503-001",
          503,
          "error.system.persistence-unavailable",
          ErrorCategory.INFRASTRUCTURE,
          true);

  private static final List<ErrorDefinition> DEFINITIONS =
      List.of(
          INPUT_INVALID,
          ENDPOINT_NOT_FOUND,
          REQUEST_METHOD_NOT_ALLOWED,
          DUPLICATE_KEY,
          OPTIMISTIC_LOCK_CONFLICT,
          MEDIA_TYPE_NOT_SUPPORTED,
          INTERNAL_ERROR,
          PERSISTENCE_UNAVAILABLE);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return DEFINITIONS;
  }

  private static ErrorDefinition error(
      String code, int status, String messageKey, ErrorCategory category, boolean retryable) {
    return new ErrorDefinition(code, status, messageKey, category, retryable);
  }
}
