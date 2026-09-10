package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.AUTHENTICATION;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class ApplicationAuthenticationException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ApplicationAuthenticationException(ErrorDefinition error) {
    super(requireCategory(error, AUTHENTICATION));
  }
}
