package com.custody.workflow.model;

import com.custody.core.exception.AppException;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.custody.workflow.model.FeatureDefinition.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record FeatureDefinition(String key, String label, List<Field> fields) {
  public record Field(String key, String label, String type, boolean required) {}

  public Map<String, String> validate(Map<String, String> input) {
    AppException.require(
        input != null
            && input.keySet().stream()
                .allMatch(
                    fieldKey ->
                        fields.stream()
                            .anyMatch(fieldDefinition -> fieldDefinition.key().equals(fieldKey))),
        WorkflowErrorCatalog.INPUT_INVALID);
    Map<String, String> result = new TreeMap<>();
    for (Field fieldDefinition : fields) {
      String fieldValue = input.getOrDefault(fieldDefinition.key(), "");
      AppException.require(fieldValue != null, WorkflowErrorCatalog.INPUT_INVALID);
      fieldValue = fieldValue.trim();
      AppException.require(
          fieldValue.length() <= 240 && (!fieldDefinition.required() || !fieldValue.isBlank()),
          WorkflowErrorCatalog.INPUT_INVALID);
      if (!fieldValue.isBlank())
        try {
          switch (fieldDefinition.type()) {
            case "date" -> LocalDate.parse(fieldValue);
            case "decimal" -> {
              BigDecimal bigDecimal = new BigDecimal(fieldValue);
              AppException.require(
                  bigDecimal.signum() >= 0
                      && bigDecimal.precision() <= 18
                      && bigDecimal.scale() <= 6,
                  WorkflowErrorCatalog.BUSINESS_RULE_VIOLATION);
              fieldValue = bigDecimal.stripTrailingZeros().toPlainString();
            }
            default -> {}
          }
        } catch (java.time.DateTimeException | NumberFormatException runtimeException) {
          throw AppException.of(WorkflowErrorCatalog.INPUT_INVALID);
        }
      result.put(fieldDefinition.key(), fieldValue);
    }
    return Map.copyOf(result);
  }
}
