package com.custody.app.error;

import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public final class ErrorCatalogRegistry {
  private final Map<String, ErrorDefinition> definitions;

  public ErrorCatalogRegistry(List<ErrorCatalog> catalogs, MessageSource messages) {
    Map<String, ErrorDefinition> discovered = new LinkedHashMap<>();
    for (ErrorCatalog catalog : catalogs) {
      for (ErrorDefinition definition : catalog.definitions()) {
        ErrorDefinition duplicate = discovered.putIfAbsent(definition.code(), definition);
        if (duplicate != null)
          throw new IllegalStateException("Duplicate error code: " + definition.code());
        if (messages.getMessage(definition.messageKey(), null, null, Locale.ENGLISH) == null)
          throw new IllegalStateException(
              "Missing safe message for error code: " + definition.code());
      }
    }
    definitions = Map.copyOf(discovered);
  }

  public ErrorDefinition require(String code) {
    ErrorDefinition definition = definitions.get(code);
    if (definition == null) throw new IllegalArgumentException("Unknown error code");
    return definition;
  }

  public Map<String, ErrorDefinition> definitions() {
    return definitions;
  }
}
