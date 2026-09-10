package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.NOT_FOUND;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class ResourceNotFoundException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(ErrorDefinition error) {
    super(requireCategory(error, NOT_FOUND));
  }
}
