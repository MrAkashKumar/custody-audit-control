package com.custody.core.error;

import com.custody.core.enums.ErrorCategory;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class CoreErrorCatalog implements ErrorCatalog {
  public static final ErrorDefinition AUDIT_EVIDENCE_INVALID =
      new ErrorDefinition(
          "AUD-500-001", 500, "error.audit.evidence-invalid", ErrorCategory.INTERNAL, false);

  @Override
  public Collection<ErrorDefinition> definitions() {
    return List.of(AUDIT_EVIDENCE_INVALID);
  }
}
