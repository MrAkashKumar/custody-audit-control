package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.INTERNAL;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class InternalApplicationException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public InternalApplicationException(ErrorDefinition error) {
    super(requireCategory(error, INTERNAL));
  }

  public InternalApplicationException(ErrorDefinition error, Throwable cause) {
    super(requireCategory(error, INTERNAL), cause);
  }
}
