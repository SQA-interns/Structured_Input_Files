package si.konferenca.registration.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import si.konferenca.registration.service.FieldError;
import si.konferenca.registration.service.RegistrationRejectedException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void rejectedRegistrationsBecome400WithFieldErrors() {
    ResponseEntity<ErrorBody> response =
        handler.rejected(
            new RegistrationRejectedException(
                List.of(new FieldError("email", "bad"), new FieldError("optionIds", "x"))));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody())
        .isEqualTo(
            new ErrorBody(
                ApiExceptionHandler.REJECTED,
                List.of(
                    new ErrorBody.FieldErrorBody("email", "bad"),
                    new ErrorBody.FieldErrorBody("optionIds", "x"))));
  }

  @Test
  void malformedBodiesBecome400WithoutFieldErrors() {
    ResponseEntity<ErrorBody> response =
        handler.malformed(
            new HttpMessageNotReadableException("x", new MockHttpInputMessage(new byte[0])));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo(ErrorBody.of(ApiExceptionHandler.MALFORMED));
    assertThat(response.getBody().fieldErrors()).isNull();
  }

  @Test
  void springErrorResponsesKeepTheirStatusWithAGenericMessage() {
    ResponseEntity<ErrorBody> response =
        handler.unexpected(new HttpRequestMethodNotSupportedException(HttpMethod.PUT.name()));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isEqualTo(ErrorBody.of(ApiExceptionHandler.NOT_PROCESSED));
  }

  @Test
  void unexpectedErrorsBecome500WithoutDetails() {
    ResponseEntity<ErrorBody> response =
        handler.unexpected(new IllegalStateException("SQL: insert into registration … secret"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo(ErrorBody.of(ApiExceptionHandler.GENERIC_ERROR));
  }
}
