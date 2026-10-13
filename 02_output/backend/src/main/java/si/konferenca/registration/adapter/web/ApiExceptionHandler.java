package si.konferenca.registration.adapter.web;

import java.util.List;
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
import si.konferenca.registration.adapter.web.ApiResponses.FieldErrorBody;
import si.konferenca.registration.adapter.web.ApiResponses.GeneralError;
import si.konferenca.registration.adapter.web.ApiResponses.ValidationError;
import si.konferenca.registration.application.StorageFailedException;

/** Maps exceptions to the error bodies of `openapi.yaml`, without internal details (SB-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Object> unreadable(HttpMessageNotReadableException e) {
    if (RequestTooLargeException.isCause(e)) {
      return error(HttpStatus.CONTENT_TOO_LARGE, "Request too large.");
    }
    return ResponseEntity.badRequest()
        .body(ValidationError.of(List.of(new FieldErrorBody("body", "Malformed request."))));
  }

  @ExceptionHandler(RequestTooLargeException.class)
  public ResponseEntity<Object> tooLarge() {
    return error(HttpStatus.CONTENT_TOO_LARGE, "Request too large.");
  }

  /** The JSON copy or the row could not be stored; nothing remains (AC-005-03). */
  @ExceptionHandler(StorageFailedException.class)
  public ResponseEntity<Object> storageFailed(StorageFailedException e) {
    LOG.error("Registration could not be stored ({})", e.getClass().getSimpleName());
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Registration could not be stored. Please try again later.");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Object> unsupportedMediaType() {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Object> methodNotAllowed() {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed.");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Object> notFound() {
    return error(HttpStatus.NOT_FOUND, "Not found.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> unexpected(Exception e) {
    LOG.error("Unexpected error ({})", e.getClass().getName());
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
  }

  static ResponseEntity<Object> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new GeneralError(message));
  }
}
