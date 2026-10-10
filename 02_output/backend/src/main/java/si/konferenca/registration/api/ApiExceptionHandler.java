package si.konferenca.registration.api;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import si.konferenca.registration.application.CaptchaRejectedException;
import si.konferenca.registration.application.DuplicateEmailException;
import si.konferenca.registration.domain.ValidationException;

/** Maps every failure to the JSON error body without internal details (SB-07, ES-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiError> invalid(ValidationException e) {
    List<ApiError.FieldErrorBody> fields =
        e.errors().stream()
            .map(
                f ->
                    new ApiError.FieldErrorBody(
                        f.field(), f.code(), FieldErrorMessages.message(f.code())))
            .toList();
    return ResponseEntity.badRequest()
        .body(
            new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Some fields are not valid.",
                fields));
  }

  @ExceptionHandler(CaptchaRejectedException.class)
  public ResponseEntity<ApiError> captcha(CaptchaRejectedException e) {
    return fieldError(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED", "captchaToken");
  }

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ApiError> duplicate(DuplicateEmailException e) {
    return fieldError(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "email");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof PayloadTooLargeException) {
        return error(
            HttpStatus.CONTENT_TOO_LARGE, "PAYLOAD_TOO_LARGE", "The request body is too large.");
      }
    }
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "The request body is not valid.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiError> mediaType(HttpMediaTypeNotSupportedException e) {
    return error(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Send application/json.");
  }

  @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
  public ResponseEntity<ApiError> notFound(Exception e) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", "Not found.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> unexpected(Exception e) {
    String errorId = UUID.randomUUID().toString();
    LOG.error("Request failed, error id {}: {}", errorId, causeChain(e));
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "The request could not be processed. Please try again later.");
  }

  /** Exception types only: messages may contain personal data (ES-07). */
  static String causeChain(Throwable e) {
    StringBuilder chain = new StringBuilder();
    for (Throwable t = e; t != null && chain.length() < 1000; t = t.getCause()) {
      if (!chain.isEmpty()) {
        chain.append(" <- ");
      }
      chain.append(t.getClass().getName());
    }
    return chain.toString();
  }

  private static ResponseEntity<ApiError> fieldError(HttpStatus status, String code, String field) {
    String message = FieldErrorMessages.message(code);
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                status.value(),
                code,
                message,
                List.of(new ApiError.FieldErrorBody(field, code, message))));
  }

  static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
  }
}
