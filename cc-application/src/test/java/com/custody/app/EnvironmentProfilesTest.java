package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.custody.app.config.EnvironmentProfileValidator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class EnvironmentProfilesTest {
  private StandardEnvironment loadProfile(String... profiles) {
    var environment = new StandardEnvironment();
    environment.setActiveProfiles(profiles);
    ConfigDataEnvironmentPostProcessor.applyTo(environment);
    return environment;
  }

  private void validate(StandardEnvironment environment) {
    new EnvironmentProfileValidator()
        .postProcessEnvironment(environment, new SpringApplication(CustodyApplication.class));
  }

  @Test
  void defaultDevelopmentUsesOnlyH2Migrations() {
    var environment = loadProfile();
    validate(environment);
    assertThat(environment.getDefaultProfiles()).containsExactly("dev");
    assertThat(environment.getProperty("spring.datasource.driver-class-name"))
        .isEqualTo("org.h2.Driver");
    assertThat(environment.getProperty("spring.flyway.locations"))
        .isEqualTo("classpath:db/migration/h2");
  }

  @Test
  void demoIncludesDevelopmentForExistingLaunchCommands() {
    var environment = loadProfile("demo");
    validate(environment);
    assertThat(environment.getActiveProfiles()).contains("demo", "dev");
  }

  @ParameterizedTest
  @ValueSource(strings = {"sit", "uat", "prod"})
  void deployedProfilesUseMysqlAndRequireExternalSecrets(String profile) {
    var environment = loadProfile(profile);
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "test-database",
                Map.of("CC_DB_URL", "jdbc:mysql://localhost/custody_profile_test")));
    validate(environment);
    assertThat(environment.getProperty("spring.datasource.driver-class-name"))
        .isEqualTo("com.mysql.cj.jdbc.Driver");
    assertThat(environment.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
    assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("false");
    assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThatThrownBy(() -> environment.getProperty("spring.datasource.password"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void mixedEnvironmentsAndProductionDemoAreRejected() {
    assertThatThrownBy(() -> validate(loadProfile("dev", "prod")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> validate(loadProfile("demo", "prod")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deployedProfilesRejectAnH2Override() {
    var environment = loadProfile("uat");
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "invalid-database", Map.of("spring.datasource.url", "jdbc:h2:mem:forbidden")));
    assertThatThrownBy(() -> validate(environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MySQL JDBC URL");
  }
}
