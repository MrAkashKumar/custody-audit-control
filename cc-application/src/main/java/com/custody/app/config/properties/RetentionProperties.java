package com.custody.app.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("cc.retention")
@Validated
public record RetentionProperties(
    @Min(1) int years,
    @Min(1) int minimumYears,
    boolean enabled,
    boolean dryRun,
    @Min(1) @Max(10000) int batchSize,
    String cron,
    String username,
    String password) {
  public RetentionProperties {
    if (years < minimumYears)
      throw new IllegalArgumentException("Retention years must meet the configured minimum");
  }
}
