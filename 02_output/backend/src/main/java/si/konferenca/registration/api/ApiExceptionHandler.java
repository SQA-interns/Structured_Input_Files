package si.konferenca.registration.api;

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
import si.konferenca.registration.domain.CaptchaUnavailableException;
import si.konferenca.registration.domain.RegistrationRejectedException;
import si.konferenca.registration.domain.StorageFailedException;

/** Maps every exception to the error shape without internal details (SB-07, ES-07). */
@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(RegistrationRejectedException.class)
  ResponseEntity<ErrorResponse> rejected(RegistrationRejectedException e) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                "The registration was not accepted.",
                e.errors().stream()
                    .map(f -> new ErrorResponse.FieldErrorDto(f.field(), f.message()))
                    .toList()));
  }

  @ExceptionHandler(CaptchaUnavailableException.class)
  ResponseEntity<ErrorResponse> captchaUnavailable(CaptchaUnavailableException e) {
    LOG.warn("Captcha verification unavailable");
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(ErrorResponse.of("The registration cannot be checked right now. Try again later."));
  }

  @ExceptionHandler(StorageFailedException.class)
  ResponseEntity<ErrorResponse> storageFailed(StorageFailedException e) {
    LOG.error("Registration could not be stored: {}", rootCauseType(e));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("The registration could not be completed. Try again later."));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(
            new ErrorResponse(
                "The request body is not valid.",
                java.util.List.of(
                    new ErrorResponse.FieldErrorDto("body", "The request body is not valid."))));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ErrorResponse> mediaType(HttpMediaTypeNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ErrorResponse.of("Content type not supported."));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorResponse> method(HttpRequestMethodNotSupportedException e) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(ErrorResponse.of("Method not allowed."));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorResponse> notFound(NoResourceFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of("Not found."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> unexpected(Exception e) {
    LOG.error("Unexpected error: {}", e.getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("The request could not be processed."));
  }

  static String rootCauseType(Throwable e) {
    Throwable t = e;
    while (t.getCause() != null && t.getCause() != t) {
      t = t.getCause();
    }
    return t.getClass().getSimpleName();
  }
}
