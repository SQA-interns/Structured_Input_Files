package si.konferenca.registration.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import si.konferenca.registration.application.RegistrationRejectedException;

/** Maps failures to openapi.json ErrorResponse without internal details (SB-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  static final String GENERAL_ERROR =
      "Registration could not be completed. Please try again later.";

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  ResponseEntity<ApiError> rejected(RegistrationRejectedException e) {
    return ResponseEntity.badRequest()
        .body(new ApiError("The registration has errors.", e.fieldErrors()));
  }

  @ExceptionHandler({MalformedRequestException.class, HttpMessageNotReadableException.class})
  ResponseEntity<ApiError> malformed(Exception e) {
    return ResponseEntity.badRequest().body(ApiError.general("The request could not be read."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> unexpected(Exception e) {
    if (e instanceof ErrorResponse framework) {
      HttpStatusCode status = framework.getStatusCode();
      return ResponseEntity.status(status).body(ApiError.general("The request was not accepted."));
    }
    LOG.error("Request failed with {}", e.getClass().getName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiError.general(GENERAL_ERROR));
  }
}
