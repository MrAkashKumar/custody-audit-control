package com.custody.app.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("cc.audit.suspicious")
@Validated
public record SuspiciousActivityProperties(
    @Min(2) @Max(100) int failedLoginThreshold,
    @Min(60) @Max(86400) int windowSeconds,
    @Min(100) @Max(100000) int maximumTrackedSubjects) {}
