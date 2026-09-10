package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.CONFLICT;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class ConflictException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ConflictException(ErrorDefinition error) {
    super(requireCategory(error, CONFLICT));
  }
}
