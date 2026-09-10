package com.custody.app.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

class ExceptionHandlerSupportTest {
  @Test
  void recognisesSupportedUniqueConstraintSignalsThroughNestedCauses() {
    SQLException sqlException = new SQLException("duplicate", "23505", 0);
    RuntimeException wrappedFailure = new RuntimeException("persistence failure", sqlException);

    assertThat(DatabaseExceptionClassifier.isUniqueConstraintViolation(wrappedFailure)).isTrue();
    assertThat(
            DatabaseExceptionClassifier.isUniqueConstraintViolation(
                new SQLException("duplicate", "unknown", 1062)))
        .isTrue();
    assertThat(
            DatabaseExceptionClassifier.isUniqueConstraintViolation(
                new SQLException("other", "22001", 0)))
        .isFalse();
  }

  @Test
  void mapsValidationErrorsDeterministicallyWithoutNullMessages() {
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "input");
    bindingResult.addError(new FieldError("input", "scope", null, false, null, null, null));
    bindingResult.addError(
        new FieldError("input", "businessKey", "", false, null, null, "must not be blank"));

    assertThat(ValidationErrorMapper.from(bindingResult))
        .containsEntry("businessKey", "must not be blank")
        .containsEntry("scope", "invalid value");
  }
}
