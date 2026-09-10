package com.custody.workflow.service;

import com.custody.workflow.model.FeatureDefinition;
import java.util.List;

/** Public use-case boundary for the FeatureRegistry capability. */
public interface FeatureRegistry {
  List<FeatureDefinition> all();

  FeatureDefinition get(String key);
}
