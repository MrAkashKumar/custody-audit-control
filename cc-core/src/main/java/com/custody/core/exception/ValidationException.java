package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.VALIDATION;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class ValidationException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ValidationException(ErrorDefinition error) {
    super(requireCategory(error, VALIDATION));
  }
}
