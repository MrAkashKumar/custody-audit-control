package com.custody.core.exception;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorDefinition;
import java.io.Serial;
import java.util.Objects;

public abstract sealed class AppException extends RuntimeException
    permits ApplicationAuthenticationException,
        ApplicationAuthorizationException,
        BusinessRuleException,
        ConflictException,
        InfrastructureException,
        InternalApplicationException,
        ResourceNotFoundException,
        ValidationException {
  @Serial private static final long serialVersionUID = 1L;
  private final ErrorDefinition error;

  protected AppException(ErrorDefinition error) {
    super(error.code());
    this.error = error;
  }

  protected AppException(ErrorDefinition error, Throwable cause) {
    super(error.code(), cause);
    this.error = error;
  }

  public final ErrorDefinition error() {
    return error;
  }

  public final String code() {
    return error.code();
  }

  public static AppException of(ErrorDefinition error) {
    Objects.requireNonNull(error, "Error definition is required");
    return switch (error.category()) {
      case VALIDATION -> new ValidationException(error);
      case AUTHENTICATION -> new ApplicationAuthenticationException(error);
      case AUTHORIZATION -> new ApplicationAuthorizationException(error);
      case NOT_FOUND -> new ResourceNotFoundException(error);
      case CONFLICT -> new ConflictException(error);
      case BUSINESS_RULE -> new BusinessRuleException(error);
      case INFRASTRUCTURE -> new InfrastructureException(error);
      case INTERNAL -> new InternalApplicationException(error);
    };
  }

  public static void require(boolean condition, ErrorDefinition error) {
    if (!condition) throw of(error);
  }

  protected static ErrorDefinition requireCategory(
      ErrorDefinition error, ErrorCategory expectedCategory) {
    Objects.requireNonNull(error, "Error definition is required");
    if (error.category() != expectedCategory) {
      throw new IllegalArgumentException(
          "Error category must be " + expectedCategory + " for " + error.code());
    }
    return error;
  }
}
