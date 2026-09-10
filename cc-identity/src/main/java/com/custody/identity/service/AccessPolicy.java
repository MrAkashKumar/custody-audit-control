package com.custody.identity.service;

import com.custody.core.enums.Action;
import com.custody.identity.dto.ScopedGrant;
import java.util.List;
import java.util.Set;

/** Public use-case boundary for the AccessPolicy capability. */
public interface AccessPolicy {
  List<ScopedGrant> grants(String actor, Action action);

  Set<String> scopes(String actor, String feature, Action action);

  boolean allows(String actor, String feature, Action action, String scope);

  void require(String actor, String feature, Action action, String scope);

  Set<String> hiddenFields(String actor, String feature, Action action, String scope);

  void administration(String actor);
}
