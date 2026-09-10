package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.AUTHORIZATION;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class ApplicationAuthorizationException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ApplicationAuthorizationException(ErrorDefinition error) {
    super(requireCategory(error, AUTHORIZATION));
  }
}
