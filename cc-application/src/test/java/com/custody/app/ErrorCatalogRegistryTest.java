package com.custody.app;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.custody.app.error.ErrorCatalogRegistry;
import com.custody.core.enums.ErrorCategory;
import com.custody.core.error.ErrorCatalog;
import com.custody.core.error.ErrorDefinition;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class ErrorCatalogRegistryTest {
  private static final ErrorDefinition TEST_ERROR =
      new ErrorDefinition(
          "TST-400-001", 400, "error.test.invalid", ErrorCategory.VALIDATION, false);

  @Test
  void duplicateFeatureCodesFailStartupValidation() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage(TEST_ERROR.messageKey(), Locale.ENGLISH, "Safe test message");
    ErrorCatalog firstCatalog = () -> List.of(TEST_ERROR);
    ErrorCatalog secondCatalog = () -> List.of(TEST_ERROR);

    assertThatThrownBy(
            () -> new ErrorCatalogRegistry(List.of(firstCatalog, secondCatalog), messages))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Duplicate error code: TST-400-001");
  }

  @Test
  void missingSafeMessageFailsStartupValidation() {
    ErrorCatalog catalog = () -> List.of(TEST_ERROR);

    assertThatThrownBy(() -> new ErrorCatalogRegistry(List.of(catalog), new StaticMessageSource()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Missing safe message for error code: TST-400-001");
  }
}
