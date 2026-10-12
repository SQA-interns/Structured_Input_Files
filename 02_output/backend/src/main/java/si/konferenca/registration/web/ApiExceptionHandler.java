package si.konferenca.registration.web;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.domain.DuplicateEmailException;
import si.konferenca.registration.domain.FieldError;
import si.konferenca.registration.domain.ValidationException;

/** Problem responses without internal details (ES-07, SB-07; specification S3). */
@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
  static final String DUPLICATE_EMAIL = "This email address is already registered.";

  @ExceptionHandler(ValidationException.class)
  ResponseEntity<ProblemDetail> invalid(ValidationException e) {
    return fieldErrors(HttpStatus.BAD_REQUEST, e.errors());
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ResponseEntity<ProblemDetail> duplicate() {
    return fieldErrors(HttpStatus.CONFLICT, List.of(new FieldError("email", DUPLICATE_EMAIL)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetail> unreadable() {
    return fieldErrors(
        HttpStatus.BAD_REQUEST, List.of(new FieldError("body", "Malformed request body.")));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ProblemDetail> unsupportedMediaType() {
    return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ProblemDetail> methodNotAllowed() {
    return problem(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ProblemDetail> notFound() {
    return problem(HttpStatus.NOT_FOUND);
  }

  /** Everything else: storage or infrastructure failure; logged without data (ES-07). */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> failure(Exception e) {
    LOG.error("request failed: {}", e.getClass().getName());
    return problem(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  static ResponseEntity<ProblemDetail> problem(HttpStatus status) {
    return ResponseEntity.status(status).body(ProblemDetail.forStatus(status));
  }

  private static ResponseEntity<ProblemDetail> fieldErrors(
      HttpStatus status, List<FieldError> errors) {
    ProblemDetail problem = ProblemDetail.forStatus(status);
    problem.setProperty(
        "fieldErrors",
        errors.stream().map(e -> Map.of("field", e.field(), "message", e.message())).toList());
    return ResponseEntity.status(status).body(problem);
  }
}
