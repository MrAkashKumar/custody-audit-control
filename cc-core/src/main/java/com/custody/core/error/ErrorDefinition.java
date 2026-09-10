package com.custody.core.error;

import com.custody.core.enums.ErrorCategory;
import java.util.Objects;
import java.util.regex.Pattern;

public record ErrorDefinition(
    String code, int status, String messageKey, ErrorCategory category, boolean retryable) {
  private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+");

  public ErrorDefinition {
    Objects.requireNonNull(code, "Error code is required");
    Objects.requireNonNull(messageKey, "Error message key is required");
    Objects.requireNonNull(category, "Error category is required");
    if (!CODE_PATTERN.matcher(code).matches())
      throw new IllegalArgumentException("Error code must use uppercase dash-separated segments");
    if (status < 400 || status > 599)
      throw new IllegalArgumentException("Error HTTP status must be between 400 and 599");
    if (messageKey.isBlank()) throw new IllegalArgumentException("Error message key is required");
  }
}
