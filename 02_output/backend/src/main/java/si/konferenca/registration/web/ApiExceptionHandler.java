package si.konferenca.registration.web;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import si.konferenca.registration.service.RegistrationRejectedException;

/** Maps exceptions to the Error schema without internal details (SB-07, specification 4.4). */
@RestControllerAdvice
public class ApiExceptionHandler {

  static final String GENERIC_ERROR =
      "The registration could not be processed. Please try again later.";
  static final String REJECTED = "The registration has errors.";
  static final String MALFORMED = "The request body is not valid JSON.";
  static final String NOT_PROCESSED = "The request cannot be processed.";

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  ResponseEntity<ErrorBody> rejected(RegistrationRejectedException e) {
    List<ErrorBody.FieldErrorBody> fields =
        e.fieldErrors().stream()
            .map(f -> new ErrorBody.FieldErrorBody(f.field(), f.message()))
            .toList();
    return ResponseEntity.badRequest().body(new ErrorBody(REJECTED, fields));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorBody> malformed(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(ErrorBody.of(MALFORMED));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception e) {
    if (e instanceof ErrorResponse response) {
      HttpStatusCode status = response.getStatusCode();
      return ResponseEntity.status(status).body(ErrorBody.of(NOT_PROCESSED));
    }
    // Only the type: messages of persistence or I/O exceptions may contain personal data.
    LOG.error("Request failed with {}", e.getClass().getName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorBody.of(GENERIC_ERROR));
  }
}
