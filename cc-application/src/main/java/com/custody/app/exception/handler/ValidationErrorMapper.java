package com.custody.app.exception.handler;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.validation.BindingResult;

/** Converts framework validation details into the bounded public field-error contract. */
final class ValidationErrorMapper {
  private static final String DEFAULT_FIELD_MESSAGE = "invalid value";

  private ValidationErrorMapper() {}

  static Map<String, String> from(BindingResult bindingResult) {
    Objects.requireNonNull(bindingResult, "Binding result is required");
    Map<String, String> fieldErrors = new TreeMap<>();
    bindingResult
        .getFieldErrors()
        .forEach(
            fieldError ->
                fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    Objects.requireNonNullElse(
                        fieldError.getDefaultMessage(), DEFAULT_FIELD_MESSAGE)));
    return Map.copyOf(fieldErrors);
  }
}
