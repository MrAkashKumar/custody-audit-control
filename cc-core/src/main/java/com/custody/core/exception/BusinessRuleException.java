package com.custody.core.exception;

import static com.custody.core.enums.ErrorCategory.BUSINESS_RULE;

import com.custody.core.error.ErrorDefinition;
import java.io.Serial;

public final class BusinessRuleException extends AppException {
  @Serial private static final long serialVersionUID = 1L;

  public BusinessRuleException(ErrorDefinition error) {
    super(requireCategory(error, BUSINESS_RULE));
  }
}
