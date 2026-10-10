package si.konferenca.registration.api;

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

/** Maps every failure to the JSON error body without internal details (SB-07, ES-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
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

  static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
  }
}
