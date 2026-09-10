package com.custody.app.config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/** Rejects mixed database environments before datasource creation or demo seeding. */
public class EnvironmentProfileValidator implements EnvironmentPostProcessor, Ordered {
  private static final Set<String> ENVIRONMENT_PROFILES = Set.of("dev", "sit", "uat", "prod");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String[] activeProfiles = environment.getActiveProfiles();
    List<String> profiles =
        Arrays.asList(
            activeProfiles.length == 0 ? environment.getDefaultProfiles() : activeProfiles);
    long selectedEnvironments = profiles.stream().filter(ENVIRONMENT_PROFILES::contains).count();
    if (selectedEnvironments != 1) {
      throw new IllegalStateException(
          "Select exactly one environment profile: dev, sit, uat or prod.");
    }
    boolean development = profiles.contains("dev");
    if (profiles.contains("demo") && !development) {
      throw new IllegalStateException("Demo data is permitted only in the dev environment.");
    }
    String databaseUrl = environment.getProperty("spring.datasource.url");
    String expectedPrefix = development ? "jdbc:h2:" : "jdbc:mysql:";
    if (databaseUrl == null || !databaseUrl.startsWith(expectedPrefix)) {
      throw new IllegalStateException(
          development
              ? "The dev profile requires an H2 JDBC URL."
              : "SIT, UAT and prod require an externally supplied MySQL JDBC URL (CC_DB_URL).");
    }
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }
}
