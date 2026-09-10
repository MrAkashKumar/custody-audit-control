package com.custody.core.error;

import java.util.Collection;

/** Feature-owned catalogue discovered and validated at startup. */
public interface ErrorCatalog {
  Collection<ErrorDefinition> definitions();
}
