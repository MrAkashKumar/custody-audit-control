package com.custody.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.custody.app.error.SystemErrorCatalog;
import com.custody.audit.error.AuditErrorCatalog;
import com.custody.core.error.CoreErrorCatalog;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import com.custody.core.exception.AppException;
import com.custody.identity.error.IdentityErrorCatalog;
import com.custody.reporting.error.ReportingErrorCatalog;
import com.custody.workflow.error.WorkflowErrorCatalog;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.persistence.Entity;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

class ArchitectureTest {
  private static final JavaClasses PRODUCTION =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("com.custody");

  @Test
  void modulesHaveNoCycles() {
    SlicesRuleDefinition.slices()
        .matching("com.custody.(*)..")
        .should()
        .beFreeOfCycles()
        .check(PRODUCTION);
  }

  @Test
  void controllersUseContractsRatherThanImplementationOrPersistence() {
    classes()
        .that()
        .areAnnotatedWith(RestController.class)
        .should()
        .resideInAPackage("..controller..")
        .check(PRODUCTION);
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..repository..", "..service.impl..")
        .check(PRODUCTION);
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(Entity.class)
        .check(PRODUCTION);
  }

  @Test
  void coreDoesNotDependOnFeatureModules() {
    noClasses()
        .that()
        .resideInAPackage("com.custody.core..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "com.custody.workflow..",
            "com.custody.identity..",
            "com.custody.reporting..",
            "com.custody.audit..",
            "com.custody.app..")
        .check(PRODUCTION);
  }

  @Test
  void entitiesAreEncapsulatedAndRepositoriesHaveDedicatedPackages() {
    classes()
        .that()
        .areAnnotatedWith(Entity.class)
        .should()
        .resideInAPackage("..model..")
        .check(PRODUCTION);
    fields()
        .that()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(Entity.class)
        .should()
        .bePrivate()
        .check(PRODUCTION);
    classes()
        .that()
        .areAssignableTo(Repository.class)
        .and()
        .resideInAPackage("com.custody..")
        .should()
        .resideInAPackage("..repository..")
        .check(PRODUCTION);
  }

  @Test
  void serviceContractsAreInterfacesAndDoNotExposeEntities() {
    classes().that().resideInAPackage("..service").should().beInterfaces().check(PRODUCTION);
    noClasses()
        .that()
        .resideInAPackage("..service")
        .should()
        .dependOnClassesThat()
        .areAnnotatedWith(Entity.class)
        .check(PRODUCTION);
    noClasses()
        .that()
        .resideInAPackage("..service")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..repository..", "..service.impl..")
        .check(PRODUCTION);
    PRODUCTION.stream()
        .filter(
            type ->
                type.getPackageName().endsWith(".service.impl") && !type.getName().contains("$"))
        .forEach(
            type ->
                assertThat(type.getAllRawInterfaces())
                    .as(
                        "%s must implement an owned or shared core service contract",
                        type.getName())
                    .anyMatch(
                        contract ->
                            contract.getPackageName().equals("com.custody.core.service")
                                || contract
                                    .getPackageName()
                                    .equals(type.getPackageName().replace(".impl", ""))));
  }

  @Test
  void modulesDoNotReachIntoAnotherModulesPersistenceOrImplementation() {
    List<String> violations = new ArrayList<>();
    for (var origin : PRODUCTION) {
      for (var dependency : origin.getDirectDependenciesFromSelf()) {
        var target = dependency.getTargetClass();
        if (!target.getName().startsWith("com.custody.")) continue;
        String sourceModule = origin.getPackageName().split("\\.")[2];
        String targetModule = target.getPackageName().split("\\.")[2];
        if (!sourceModule.equals(targetModule)
            && (target.getPackageName().contains(".repository")
                || target.getPackageName().contains(".service.impl")
                || target.isAnnotatedWith(Entity.class))) {
          violations.add(dependency.getDescription());
        }
      }
    }
    assertThat(violations).isEmpty();
  }

  @Test
  void requestAndResponseDtosAreStandaloneRecords() {
    PRODUCTION.stream()
        .filter(
            type ->
                type.getPackageName().endsWith(".dto.request")
                    || type.getPackageName().endsWith(".dto.response"))
        .forEach(
            type -> {
              assertThat(type.isRecord()).as(type.getName()).isTrue();
              assertThat(type.getEnclosingClass()).as(type.getName()).isEmpty();
            });
    noClasses()
        .that()
        .resideInAPackage("..dto..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..repository..", "..service.impl..")
        .check(PRODUCTION);
  }

  @Test
  void productionTypesHaveResponsibilityPackages() {
    PRODUCTION.stream()
        .filter(type -> !type.getName().contains("$"))
        .filter(type -> !type.getSimpleName().equals("CustodyApplication"))
        .forEach(
            type ->
                assertThat(type.getPackageName().split("\\.").length)
                    .as(type.getName())
                    .isGreaterThanOrEqualTo(4));
    classes().that().areEnums().should().resideInAPackage("..enums..").check(PRODUCTION);
  }

  @Test
  void auditReadAndWriteContractsRemainSeparated() {
    assertThat(
            com.custody.core.service.AuditWriter.class.isAssignableFrom(
                com.custody.audit.service.AuditService.class))
        .isFalse();
    noClasses()
        .that()
        .resideInAnyPackage("com.custody.workflow..", "com.custody.identity..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.custody.audit..")
        .check(PRODUCTION);
  }

  @Test
  void featureErrorCataloguesHaveUniqueCustomCodesAndBoundedExceptionCategories() {
    List<ErrorCatalog> catalogs =
        List.of(
            new CoreErrorCatalog(),
            new SystemErrorCatalog(),
            new IdentityErrorCatalog(),
            new AuditErrorCatalog(),
            new WorkflowErrorCatalog(),
            new ReportingErrorCatalog());
    List<ErrorDefinition> definitions =
        catalogs.stream().flatMap(catalog -> catalog.definitions().stream()).toList();
    List<AppException> exceptions = definitions.stream().map(AppException::of).toList();

    assertThat(definitions).extracting(ErrorDefinition::code).doesNotHaveDuplicates();
    assertThat(definitions).extracting(ErrorDefinition::messageKey).doesNotHaveDuplicates();
    assertThat(exceptions).extracting(AppException::error).containsExactlyElementsOf(definitions);
    assertThat(exceptions)
        .extracting(AppException::getClass)
        .allSatisfy(
            type -> {
              assertThat(type.getPackageName()).isEqualTo("com.custody.core.exception");
              assertThat(type.getSimpleName()).endsWith("Exception");
              assertThat(Modifier.isFinal(type.getModifiers())).isTrue();
            });
    assertThat(exceptions.stream().map(AppException::getClass).distinct()).hasSize(8);
  }
}
