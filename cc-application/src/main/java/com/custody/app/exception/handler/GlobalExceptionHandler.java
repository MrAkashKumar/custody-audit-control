package com.custody.app.exception.handler;

import com.custody.app.dto.response.ErrorResponse;
import com.custody.app.error.SystemErrorCatalog;
import com.custody.core.exception.AppException;
import com.custody.identity.error.IdentityErrorCatalog;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final ErrorResponseFactory errors;

  GlobalExceptionHandler(ErrorResponseFactory errorResponseFactory) {
    this.errors = errorResponseFactory;
  }

  @ExceptionHandler(AppException.class)
  ResponseEntity<ErrorResponse> handleApplicationException(AppException appException) {
    log.info(
        "event=application_exception exception={} code={}",
        appException.getClass().getSimpleName(),
        appException.code());
    return errors.response(appException.error(), Map.of());
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
  ResponseEntity<ErrorResponse> handleBindingFailure(BindException bindException) {
    Map<String, String> fields = ValidationErrorMapper.from(bindException.getBindingResult());
    log.info("event=request_binding_failed fields={}", fields.keySet());
    return errors.response(SystemErrorCatalog.INPUT_INVALID, fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    ConstraintViolationException.class
  })
  ResponseEntity<ErrorResponse> handleInvalidInput(Exception exception) {
    log.info("event=request_input_invalid exception={}", exception.getClass().getSimpleName());
    return errors.response(SystemErrorCatalog.INPUT_INVALID, Map.of());
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorResponse> handleEndpointNotFound() {
    return errors.response(SystemErrorCatalog.ENDPOINT_NOT_FOUND, Map.of());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorResponse> handleMethodNotAllowed() {
    return errors.response(SystemErrorCatalog.REQUEST_METHOD_NOT_ALLOWED, Map.of());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorResponse> handleMediaTypeNotSupported() {
    return errors.response(SystemErrorCatalog.MEDIA_TYPE_NOT_SUPPORTED, Map.of());
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<ErrorResponse> handleAccessDenied() {
    return errors.response(IdentityErrorCatalog.ACTION_FORBIDDEN, Map.of());
  }

  @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
  ResponseEntity<ErrorResponse> handleStaleVersion() {
    return errors.response(SystemErrorCatalog.OPTIMISTIC_LOCK_CONFLICT, Map.of());
  }

  @ExceptionHandler(DuplicateKeyException.class)
  ResponseEntity<ErrorResponse> handleDuplicateKey() {
    return errors.response(SystemErrorCatalog.DUPLICATE_KEY, Map.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorResponse> handleDataConflict(DataIntegrityViolationException exception) {
    if (DatabaseExceptionClassifier.isUniqueConstraintViolation(exception)) {
      return errors.response(SystemErrorCatalog.DUPLICATE_KEY, Map.of());
    }
    log.error("event=unclassified_integrity_failure", exception);
    return errors.response(SystemErrorCatalog.INTERNAL_ERROR, Map.of());
  }

  @ExceptionHandler({
    TransientDataAccessException.class,
    org.springframework.transaction.CannotCreateTransactionException.class
  })
  ResponseEntity<ErrorResponse> handlePersistenceUnavailable(Exception exception) {
    log.error("event=infrastructure_failure", exception);
    return errors.response(SystemErrorCatalog.PERSISTENCE_UNAVAILABLE, Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> handleUnexpectedFailure(Exception exception) {
    log.error("event=unexpected_failure", exception);
    return errors.response(SystemErrorCatalog.INTERNAL_ERROR, Map.of());
  }
}
