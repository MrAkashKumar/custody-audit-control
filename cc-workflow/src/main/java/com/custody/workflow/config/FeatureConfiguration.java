package com.custody.workflow.config;

import com.custody.workflow.model.FeatureDefinition;
import com.custody.workflow.model.FeatureDefinition.Field;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FeatureConfiguration {
  private Field text(String fieldKey, String fieldLabel) {
    return new Field(fieldKey, fieldLabel, "text", true);
  }

  @Bean
  FeatureDefinition holiday() {
    return new FeatureDefinition(
        "holiday-calendar",
        "Holiday Calendar",
        List.of(
            text("name", "Holiday name"),
            new Field("date", "Holiday date", "date", true),
            text("market", "Market")));
  }

  @Bean
  FeatureDefinition gold() {
    return new FeatureDefinition(
        "physical-gold",
        "Physical Gold",
        List.of(
            text("name", "Product name"),
            new Field("purity", "Purity (%)", "decimal", true),
            text("unit", "Unit")));
  }

  @Bean
  FeatureDefinition loco() {
    return new FeatureDefinition(
        "loco-singapore",
        "Loco Singapore",
        List.of(
            text("name", "Location name"),
            text("settlementCode", "Settlement code"),
            text("currency", "Currency")));
  }

  @Bean
  FeatureDefinition vault() {
    return new FeatureDefinition(
        "vault",
        "Vault",
        List.of(
            text("name", "Vault name"),
            text("location", "Location"),
            text("operator", "Operator")));
  }

  @Bean
  FeatureDefinition fees() {
    return new FeatureDefinition(
        "fee-schedule",
        "Fee Schedule",
        List.of(
            text("name", "Schedule name"),
            new Field("rate", "Rate", "decimal", true),
            text("currency", "Currency"),
            new Field("effectiveDate", "Effective date", "date", true)));
  }

  @Bean
  FeatureDefinition reference() {
    return new FeatureDefinition(
        "reference-data",
        "Managed Reference Data",
        List.of(
            text("name", "Display name"), text("category", "Category"), text("value", "Value")));
  }
}
