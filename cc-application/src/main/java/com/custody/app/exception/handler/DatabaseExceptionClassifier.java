package com.custody.app.exception.handler;

import java.sql.SQLException;
import java.util.Set;

/** Classifies portable database constraint failures without leaking database details. */
final class DatabaseExceptionClassifier {
  private static final String UNIQUE_CONSTRAINT_SQL_STATE = "23505";
  private static final Set<Integer> UNIQUE_CONSTRAINT_VENDOR_CODES = Set.of(1, 1062, 2601, 2627);

  private DatabaseExceptionClassifier() {}

  static boolean isUniqueConstraintViolation(Throwable failure) {
    Throwable currentCause = failure;
    while (currentCause != null) {
      if (currentCause instanceof SQLException sqlException
          && (UNIQUE_CONSTRAINT_SQL_STATE.equals(sqlException.getSQLState())
              || UNIQUE_CONSTRAINT_VENDOR_CODES.contains(sqlException.getErrorCode()))) {
        return true;
      }
      currentCause = currentCause.getCause();
    }
    return false;
  }
}
