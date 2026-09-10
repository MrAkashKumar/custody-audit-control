package com.custody.reporting.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("cc.reports")
@Validated
public record ReportProperties(@Min(1) int expiryDays, @Min(1) @Max(100000) int maxRows) {}
