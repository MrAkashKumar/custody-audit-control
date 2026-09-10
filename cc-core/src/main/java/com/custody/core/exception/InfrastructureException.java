package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.INFRASTRUCTURE;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class InfrastructureException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public InfrastructureException(ErrorDefinition error) {
    super(requireCategory(error, INFRASTRUCTURE));
  }

  public InfrastructureException(ErrorDefinition error, Throwable cause) {
    super(requireCategory(error, INFRASTRUCTURE), cause);
  }
}
