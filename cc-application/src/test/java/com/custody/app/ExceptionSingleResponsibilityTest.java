package com.custody.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorDefinition;
import com.custody.core.exception.AppException;
import com.custody.core.exception.ConflictException;
import com.custody.core.exception.ValidationException;
import org.junit.jupiter.api.Test;

class ExceptionSingleResponsibilityTest {
  private static final ErrorDefinition VALIDATION_ERROR =
      new ErrorDefinition(
          "TST-400-001", 400, "error.test.validation", ErrorCategory.VALIDATION, false);
  private static final ErrorDefinition CONFLICT_ERROR =
      new ErrorDefinition("TST-409-001", 409, "error.test.conflict", ErrorCategory.CONFLICT, false);

  @Test
  void factoryPreservesTheExactFeatureCodeAndSelectsItsCategoryException() {
    AppException exception = AppException.of(VALIDATION_ERROR);

    assertThat(exception)
        .isInstanceOf(ValidationException.class)
        .extracting(AppException::error)
        .isSameAs(VALIDATION_ERROR);
    assertThat(exception.code()).isEqualTo("TST-400-001");
  }

  @Test
  void categorySpecificExceptionsRejectDefinitionsFromAnotherCategory() {
    assertThatThrownBy(() -> new ConflictException(VALIDATION_ERROR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Error category must be CONFLICT for TST-400-001");

    assertThat(new ConflictException(CONFLICT_ERROR).error()).isSameAs(CONFLICT_ERROR);
  }
}
