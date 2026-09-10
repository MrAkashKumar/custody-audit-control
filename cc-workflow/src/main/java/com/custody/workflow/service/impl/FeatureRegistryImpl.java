package com.custody.workflow.service.impl;

import com.custody.core.exception.AppException;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.model.FeatureDefinition;
import com.custody.workflow.service.FeatureRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FeatureRegistryImpl implements FeatureRegistry {
  private final Map<String, FeatureDefinition> definitions;

  public FeatureRegistryImpl(List<FeatureDefinition> features) {
    Map<String, FeatureDefinition> map = new LinkedHashMap<>();
    for (var featureDefinition : features)
      if (map.put(featureDefinition.key(), featureDefinition) != null)
        throw new IllegalStateException("Duplicate feature key");
    definitions = Collections.unmodifiableMap(map);
  }

  @Override
  public List<FeatureDefinition> all() {
    return List.copyOf(definitions.values());
  }

  @Override
  public FeatureDefinition get(String key) {
    var featureDefinition = definitions.get(key);
    if (featureDefinition == null)
      throw AppException.of(WorkflowErrorCatalog.RESOURCE_NOT_AVAILABLE);
    return featureDefinition;
  }
}
